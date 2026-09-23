package it.unict.smartuniversity.web_portal.controller;

import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Controller
public class AdminWebController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${backend.auth-service.url}")
    private String authServiceUrl;

    @Value("${backend.student-service.url}")
    private String studentServiceUrl;

    @Value("${backend.exam-service.url}")
    private String examServiceUrl;

    @GetMapping("/admin/home")
    public String showAdminHome(HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");
        String username = (String) session.getAttribute("username");

        if (token == null || !"ADMIN".equalsIgnoreCase(role)) {
            session.invalidate();
            model.addAttribute("error", "Accesso negato! Area riservata agli amministratori.");
            return "redirect:/login";
        }

        model.addAttribute("username", username);
        return "admin-home";
    }

    @PostMapping("/admin/register-user")
    public String registerUser(
            @RequestParam("newUsername") String newUsername,
            @RequestParam("newPassword") String newPassword,
            @RequestParam("newRole") String newRole,
            @RequestParam(value = "nome", required = false) String nome,
            @RequestParam(value = "cognome", required = false) String cognome,
            @RequestParam(value = "matricola", required = false) String matricola,
            @RequestParam(value = "email", required = false) String email,
            @RequestParam(value = "corsoDiLaurea", required = false) String corsoDiLaurea,
            HttpSession session,
            Model model) {

        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");
        String username = (String) session.getAttribute("username");

        if (token == null || !"ADMIN".equalsIgnoreCase(role)) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            // 1. REGISTRAZIONE SU AUTH-SERVICE
            Map<String, Object> authBody = new HashMap<>();
            authBody.put("username", newUsername);
            authBody.put("password", newPassword);
            authBody.put("role", newRole);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            HttpEntity<Map<String, Object>> authEntity = new HttpEntity<>(authBody, headers);
            String authEndpoint = authServiceUrl + "/register"; 
            ResponseEntity<Map> authResponse = restTemplate.postForEntity(authEndpoint, authEntity, Map.class);

            if (authResponse.getStatusCode().is2xxSuccessful()) {
                
                // 2. PROPAGAZIONE SU STUDENT-SERVICE SE STUDENTE
                if ("STUDENTE".equalsIgnoreCase(newRole)) {
                    Map<String, Object> studentBody = new HashMap<>();
                    studentBody.put("username", newUsername);
                    studentBody.put("nome", nome);
                    studentBody.put("cognome", cognome);
                    studentBody.put("matricola", matricola);
                    studentBody.put("email", email);
                    studentBody.put("corsoDiLaurea", corsoDiLaurea);

                    HttpEntity<Map<String, Object>> studentEntity = new HttpEntity<>(studentBody, headers);
                    ResponseEntity<Map> studentResponse = restTemplate.postForEntity(studentServiceUrl, studentEntity, Map.class);

                    if (!studentResponse.getStatusCode().is2xxSuccessful()) {
                        model.addAttribute("error", "Credenziali create, ma fallita la sincronizzazione anagrafica nello student-service.");
                        model.addAttribute("username", username);
                        return "admin-home";
                    }
                }

                model.addAttribute("success", "Utente '" + newUsername + "' registrato con successo!");
            } else {
                model.addAttribute("error", "Errore durante la registrazione delle credenziali.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Errore di sincronizzazione distribuita: " + e.getMessage());
        }

        model.addAttribute("username", username);
        return "admin-home";
    }

    /**
     * Gestisce l'attivazione di un insegnamento (materia) nell'offerta formativa.
     * CONTROLLO: Verifica che l'utente a cui si assegna la materia sia effettivamente un DOCENTE.
     */
    @PostMapping("/admin/create-course")
    public String createInsegnamento(
            @RequestParam("codice") String codice,
            @RequestParam("nome") String nome,
            @RequestParam("docente") String docente,
            HttpSession session,
            Model model) {

        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");
        String username = (String) session.getAttribute("username");

        if (token == null || !"ADMIN".equalsIgnoreCase(role)) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("Authorization", "Bearer " + token);

            // 1. VERIFICA DEL RUOLO SU AUTH-SERVICE
            try {
                String roleCheckUrl = authServiceUrl + "/users/" + docente + "/role";
                HttpEntity<Void> checkEntity = new HttpEntity<>(headers);
                ResponseEntity<Map> checkResponse = restTemplate.exchange(roleCheckUrl, HttpMethod.GET, checkEntity, Map.class);

                if (checkResponse.getStatusCode().is2xxSuccessful() && checkResponse.getBody() != null) {
                    String assignedRole = (String) checkResponse.getBody().get("role");
                    if (!"DOCENTE".equalsIgnoreCase(assignedRole)) {
                        model.addAttribute("error", "Impossibile assegnare il corso: l'utente '" + docente + "' non è un DOCENTE (ruolo attuale: " + assignedRole + ").");
                        model.addAttribute("username", username);
                        return "admin-home";
                    }
                }
            } catch (org.springframework.web.client.HttpClientErrorException.NotFound e) {
                model.addAttribute("error", "Impossibile assegnare il corso: l'utente '" + docente + "' non esiste nel sistema!");
                model.addAttribute("username", username);
                return "admin-home";
            }

            // 2. CREAZIONE DEL CORSO SU EXAM-SERVICE
            Map<String, Object> requestBody = new HashMap<>();
            requestBody.put("codice", codice);
            requestBody.put("nome", nome);
            requestBody.put("docente", docente);

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(requestBody, headers);

            String endpoint = examServiceUrl + "/courses"; 
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful() || response.getStatusCode().value() == 201) {
                model.addAttribute("success", "Insegnamento '" + nome + "' attivato con successo e assegnato al docente " + docente + "!");
            } else {
                model.addAttribute("error", "Errore del server durante l'attivazione del corso.");
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            model.addAttribute("error", "Errore dal servizio esami: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            model.addAttribute("error", "Errore di comunicazione: " + e.getMessage());
        }

        model.addAttribute("username", username);
        return "admin-home";
    }
}
