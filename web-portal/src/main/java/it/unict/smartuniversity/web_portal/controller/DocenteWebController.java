package it.unict.smartuniversity.web_portal.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DocenteWebController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${backend.exam-service.url}")
    private String examServiceUrl;

    @GetMapping("/docente/home")
    public String showDocenteHome(HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");
        String username = (String) session.getAttribute("username");

        if (token == null || !"DOCENTE".equalsIgnoreCase(role)) {
            session.invalidate();
            model.addAttribute("error", "Accesso negato!");
            return "redirect:/login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 1. Carica i corsi del docente
            ResponseEntity<List<Map>> coursesResponse = restTemplate.exchange(
                    examServiceUrl + "/courses/my",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map>>() {}
            );
            model.addAttribute("courses", coursesResponse.getBody());

            // 2. Carica gli appelli creati
            ResponseEntity<List<Map>> callsResponse = restTemplate.exchange(
                    examServiceUrl + "/calls",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map>>() {}
            );
            
            List<Map> myCalls = new ArrayList<>();
            if (callsResponse.getBody() != null) {
                for (Map call : callsResponse.getBody()) {
                    if (username.equalsIgnoreCase((String) call.get("docente"))) {
                        myCalls.add(call);
                    }
                }
            }
            model.addAttribute("appelli", myCalls);

            // 3. Carica le prenotazioni attive DA VERBALIZZARE
            ResponseEntity<List<Map>> pendingResponse = restTemplate.exchange(
                    examServiceUrl + "/docente/pending-bookings",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map>>() {}
            );
            model.addAttribute("pendingBookings", pendingResponse.getBody());

        } catch (Exception e) {
            model.addAttribute("error", "Errore nel caricamento dei dati: " + e.getMessage());
        }

        model.addAttribute("username", username);
        return "docente-home";
    }

    @PostMapping("/docente/create-call")
    public String createExamCall(
            @RequestParam("insegnamentoId") Long insegnamentoId,
            @RequestParam("dataEsame") String dataEsame,
            HttpSession session,
            Model model) {

        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");

        if (token == null || !"DOCENTE".equalsIgnoreCase(role)) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("insegnamentoId", insegnamentoId);
            requestBody.put("dataEsame", dataEsame);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            String endpoint = examServiceUrl + "/calls";
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 201) {
                model.addAttribute("success", "Appello d'esame ufficiale programmato con successo!");
            } else {
                model.addAttribute("error", "Impossibile pubblicare l'appello d'esame.");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            model.addAttribute("error", "Errore dal servizio esami: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            model.addAttribute("error", "Errore di comunicazione: " + e.getMessage());
        }

        return showDocenteHome(session, model);
    }

    @PostMapping("/docente/close-call")
    public String closeExamCall(
            @RequestParam("callId") Long callId,
            HttpSession session,
            Model model) {

        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");

        if (token == null || !"DOCENTE".equalsIgnoreCase(role)) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            String endpoint = examServiceUrl + "/calls/" + callId + "/close";
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("success", "L'appello d'esame è stato chiuso con successo!");
            } else {
                model.addAttribute("error", "Impossibile chiudere l'appello.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Errore durante la chiusura dell'appello: " + e.getMessage());
        }

        return showDocenteHome(session, model);
    }

    @PostMapping("/docente/verbalizza")
    public String verbalizzaVoto(
            @RequestParam("bookingId") Long bookingId,
            @RequestParam("esito") String esito,
            @RequestParam(value = "voto", required = false) Integer voto,
            @RequestParam(value = "lode", required = false) Boolean lode,
            HttpSession session, Model model) {

        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");

        if (token == null || !"DOCENTE".equalsIgnoreCase(role)) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            headers.setContentType(MediaType.APPLICATION_JSON); // CORRETTO: JSON payload

            Map<String, Object> body = new HashMap<>();
            body.put("bookingId", bookingId);
            body.put("esito", esito);
            body.put("voto", "PROMOSSO".equalsIgnoreCase(esito) ? (voto != null ? voto : 18) : 0);
            body.put("lode", "PROMOSSO".equalsIgnoreCase(esito) && Boolean.TRUE.equals(lode));

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(body, headers);

            // CORRETTO: Endpoint /grades
            String endpoint = examServiceUrl + "/grades";
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("success", "Verbalizzazione (" + esito + ") completata con successo!");
            } else {
                model.addAttribute("error", "Errore durante la verbalizzazione.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Errore durante la verbalizzazione: " + e.getMessage());
        }

        return showDocenteHome(session, model);
    }
}
