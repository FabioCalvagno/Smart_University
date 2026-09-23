package it.unict.smartuniversity.web_portal.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpSession;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.client.RestTemplate;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Controller
public class LoginController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${backend.auth-service.url}")
    private String authServiceUrl;

    @GetMapping("/login")
    public String showLoginPage(HttpSession session, Model model) {
        String role = (String) session.getAttribute("role");
        if (role != null) {
            if ("STUDENTE".equalsIgnoreCase(role)) return "redirect:/student/home";
            if ("DOCENTE".equalsIgnoreCase(role)) return "redirect:/docente/home";
            if ("ADMIN".equalsIgnoreCase(role)) return "redirect:/admin/home";
        }
        return "login";
    }

    @PostMapping("/login")
    public String handleLogin(
            @RequestParam("username") String username,
            @RequestParam("password") String password,
            HttpSession session,
            Model model) {

        try {
            Map<String, String> loginRequest = new HashMap<>();
            loginRequest.put("username", username);
            loginRequest.put("password", password);

            String endpoint = authServiceUrl + "/login";
            ResponseEntity<Map> responseEntity = restTemplate.postForEntity(endpoint, loginRequest, Map.class);

            if (responseEntity.getStatusCode().is2xxSuccessful() && responseEntity.getBody() != null) {
                Map<String, Object> body = responseEntity.getBody();
                String token = (String) body.get("token");

                if (token != null) {
                    // --- DECODIFICA DEL JWT SENZA LIBRERIE ESTERNE ---
                    // Un JWT è composto da: Header.Payload.Signature
                    String[] jwtParts = token.split("\\.");
                    String payloadBase64 = jwtParts[1];
                    
                    // Decodifichiamo la parte centrale (Payload)
                    String payloadJson = new String(Base64.getUrlDecoder().decode(payloadBase64));
                    
                    // Convertiamo la stringa JSON in una Mappa Java usando Jackson (ObjectMapper)
                    ObjectMapper mapper = new ObjectMapper();
                    Map<String, Object> claims = mapper.readValue(payloadJson, Map.class);

                    // Estraiamo i dati reali dal Token
                    String role = (String) claims.get("role");
                    String tokenUsername = (String) claims.get("sub"); // Lo standard JWT salva l'utente nel subject ("sub")

                    // Salviamo i dati decodificati nel Server Session State (HttpSession)
                    session.setAttribute("token", token);
                    session.setAttribute("username", tokenUsername != null ? tokenUsername : username);
                    session.setAttribute("role", role);

                    System.out.println("🔑 Login riuscito per " + tokenUsername + ". Ruolo estratto dal JWT: " + role);

                    // Reindirizzamento basato su RBAC
                    if ("STUDENTE".equalsIgnoreCase(role)) {
                        return "redirect:/student/home";
                    } else if ("DOCENTE".equalsIgnoreCase(role)) {
                        return "redirect:/docente/home";
                    } else if ("ADMIN".equalsIgnoreCase(role)) {
                        return "redirect:/admin/home";
                    }
                }
            }

            model.addAttribute("error", "Credenziali non valide.");
            return "login";

        } catch (Exception e) {
            model.addAttribute("error", "Accesso negato! Controlla username e password.");
            return "login";
        }
    }

    @GetMapping("/logout")
    public String handleLogout(HttpSession session, Model model) {
        session.invalidate();
        model.addAttribute("message", "Disconnessione avvenuta con successo.");
        return "login";
    }
}