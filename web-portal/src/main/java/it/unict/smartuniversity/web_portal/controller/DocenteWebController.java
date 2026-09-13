package it.unict.smartuniversity.web_portal.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
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

            // 1. Carica i corsi assegnati a questo docente
            ResponseEntity<List<Map>> coursesResponse = restTemplate.exchange(
                    examServiceUrl + "/courses/my",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map>>() {}
            );
            model.addAttribute("courses", coursesResponse.getBody());

            // 2. Carica tutti gli appelli d'esame assegnati a questo docente
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

        } catch (Exception e) {
            model.addAttribute("error", "Errore nel caricamento delle materie del docente: " + e.getMessage());
        }

        model.addAttribute("username", username);
        return "docente-home";
    }

    /**
     * CORRETTO: Il docente crea un appello per un suo corso.
     */
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

            Map<String, Object> insegnamentoMap = new HashMap<>();
            insegnamentoMap.put("id", insegnamentoId);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("insegnamento", insegnamentoMap);
            requestBody.put("dataEsame", dataEsame);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            String endpoint = examServiceUrl + "/calls";
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 201) {
                model.addAttribute("success", "Appello d'esame ufficiale programmato con successo!");
            } else {
                model.addAttribute("error", "Impossibile pubblicare l'appello d'esame.");
            }
        } catch (Exception e) {
            model.addAttribute("error", "Errore di comunicazione: " + e.getMessage());
        }

        return showDocenteHome(session, model);
    }

    @PostMapping("/docente/verbalizza")
    public String handleVerbalizzazione(
            @RequestParam("matricola") String matricola,
            @RequestParam("nomeInsegnamento") String nomeInsegnamento,
            @RequestParam("voto") Integer voto,
            @RequestParam(value = "lode", defaultValue = "false") Boolean lode,
            HttpSession session,
            Model model) {

        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");
        String username = (String) session.getAttribute("username");

        if (token == null || !"DOCENTE".equalsIgnoreCase(role)) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("matricola", matricola);
            requestBody.put("nomeInsegnamento", nomeInsegnamento);
            requestBody.put("voto", voto);
            requestBody.put("lode", lode);
            requestBody.put("docente", username);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            String endpoint = examServiceUrl + "/grades"; 
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("success", "Voto inserito per la verbalizzazione asincrona con successo!");
            } else {
                model.addAttribute("error", "Errore durante la verbalizzazione del voto.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Errore di comunicazione: " + e.getMessage());
        }

        return showDocenteHome(session, model);
    }
}