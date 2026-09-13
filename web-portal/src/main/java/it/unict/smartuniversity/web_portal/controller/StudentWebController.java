package it.unict.smartuniversity.web_portal.controller;

import it.unict.smartuniversity.web_portal.dto.GradeDTO;
import jakarta.servlet.http.HttpSession;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.vavr.control.Try;
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

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

@Controller
public class StudentWebController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${backend.student-service.url}")
    private String studentServiceUrl;

    @Value("${backend.exam-service.url}")
    private String examServiceUrl;

    private final CircuitBreaker circuitBreaker = CircuitBreaker.of("studentServiceCB", 
        CircuitBreakerConfig.custom()
            .slidingWindowSize(5)
            .failureRateThreshold(50)
            .waitDurationInOpenState(Duration.ofSeconds(10))
            .build()
    );

    @GetMapping("/student/home")
    public String showStudentHome(HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");
        String username = (String) session.getAttribute("username");

        if (token == null || !"STUDENTE".equalsIgnoreCase(role)) {
            session.invalidate();
            model.addAttribute("error", "Accesso negato! Effettua prima il login.");
            return "redirect:/login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 1. Carica gli appelli d'esame disponibili per la prenotazione
            ResponseEntity<List<Map>> callsResponse = restTemplate.exchange(
                    examServiceUrl + "/calls",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map>>() {}
            );
            model.addAttribute("appelli", callsResponse.getBody());

            // 2. Carica le prenotazioni attive dello studente
            ResponseEntity<List<Map>> bookingsResponse = restTemplate.exchange(
                    examServiceUrl + "/my-bookings",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map>>() {}
            );
            model.addAttribute("prenotazioni", bookingsResponse.getBody());

        } catch (Exception e) {
            model.addAttribute("error", "Errore nel caricamento delle prenotazioni: " + e.getMessage());
        }

        // 3. Carica il libretto dei voti protetto dal Circuit Breaker
        Supplier<List<GradeDTO>> restCallSupplier = () -> {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            String endpoint = studentServiceUrl + "/me/grades";
            ResponseEntity<List<GradeDTO>> responseEntity = restTemplate.exchange(
                    endpoint,
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<GradeDTO>>() {}
            );
            return responseEntity.getBody();
        };

        List<GradeDTO> libretto = Try.ofSupplier(CircuitBreaker.decorateSupplier(circuitBreaker, restCallSupplier))
                .recover(throwable -> {
                    System.err.println("⚠️ [Resilience4J CB] Fallback attivo: " + throwable.getMessage());
                    model.addAttribute("warning", "Il servizio carriere di Ateneo è momentaneamente offline per manutenzione. I tuoi voti sono al sicuro.");
                    return new ArrayList<>();
                })
                .get();

        model.addAttribute("username", username);
        model.addAttribute("libretto", libretto);

        return "student-home";
    }

    /**
     * Gestisce la prenotazione dinamica di uno studente a un appello d'esame.
     */
    @PostMapping("/student/book")
    public String bookExamCall(@RequestParam("callId") Long callId, HttpSession session, Model model) {
        String token = (String) session.getAttribute("token");
        String role = (String) session.getAttribute("role");

        if (token == null || !"STUDENTE".equalsIgnoreCase(role)) {
            session.invalidate();
            return "redirect:/login";
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // Chiamata sincrona a student-service per recuperare l'anagrafica del profilo corrente
            ResponseEntity<Map> infoResponse = restTemplate.exchange(
                    studentServiceUrl + "/me",
                    HttpMethod.GET,
                    requestEntity,
                    Map.class
            );

            String matricola = "M81001234"; // Valore di fallback preventivo
            if (infoResponse.getBody() != null && infoResponse.getBody().get("matricola") != null) {
                matricola = (String) infoResponse.getBody().get("matricola");
            }

            // Invio della prenotazione sincrona con header aggiuntivo contenente la matricola
            HttpHeaders bookHeaders = new HttpHeaders();
            bookHeaders.set("Authorization", "Bearer " + token);
            bookHeaders.set("X-Student-Matricola", matricola);
            HttpEntity<Void> bookEntity = new HttpEntity<>(bookHeaders);

            String bookEndpoint = examServiceUrl + "/calls/" + callId + "/book";
            ResponseEntity<Map> bookResponse = restTemplate.postForEntity(bookEndpoint, bookEntity, Map.class);

            if (bookResponse.getStatusCode().is2xxSuccessful() || bookResponse.getStatusCode().value() == 201) {
                model.addAttribute("success", "Ti sei prenotato con successo all'appello d'esame!");
            } else {
                model.addAttribute("error", "Impossibile completare la prenotazione.");
            }

        } catch (Exception e) {
            model.addAttribute("error", "Errore durante la prenotazione: " + e.getMessage());
        }

        return showStudentHome(session, model);
    }
}