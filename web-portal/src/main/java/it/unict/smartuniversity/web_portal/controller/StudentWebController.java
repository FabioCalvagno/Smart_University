package it.unict.smartuniversity.web_portal.controller;

import it.unict.smartuniversity.web_portal.dto.GradeDTO;
import jakarta.servlet.http.HttpSession;
import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.vavr.control.Try;
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

import java.time.Duration;
import java.util.*;
import java.util.function.Supplier;

@Controller
public class StudentWebController {

    @Autowired
    private RestTemplate restTemplate;

    @Value("${backend.student-service.url}")
    private String studentServiceUrl;

    @Value("${backend.exam-service.url}")
    private String examServiceUrl;

    // Configurazione del Circuit Breaker di Resilience4j
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

        List<Map> myBookings = new ArrayList<>();
        Set<Long> bookedCallIds = new HashSet<>();
        Set<Long> passedInsegnamentoIds = new HashSet<>();
        Set<String> passedCoursesSet = new HashSet<>();

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            // 1. Carica le prenotazioni attive dello studente da exam-service
            ResponseEntity<List<Map>> bookingsResponse = restTemplate.exchange(
                    examServiceUrl + "/my-bookings",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map>>() {}
            );
            if (bookingsResponse.getBody() != null) {
                myBookings = bookingsResponse.getBody();
                for (Map booking : myBookings) {
                    Map examCall = (Map) booking.get("examCall");
                    if (examCall != null) {
                        if (examCall.get("id") != null) {
                            bookedCallIds.add(Long.valueOf(examCall.get("id").toString()));
                        }
                        Map insegnamento = (Map) examCall.get("insegnamento");
                        if (insegnamento != null) {
                            Long insId = (insegnamento.get("id") != null) ? Long.valueOf(insegnamento.get("id").toString()) : null;
                            String nomeIns = (String) insegnamento.get("nome");

                            Boolean verbalizzata = (Boolean) booking.get("verbalizzata");
                            String stato = (String) booking.get("stato");

                            // Rileva le materie già verificate e verbalizzate
                            if (Boolean.TRUE.equals(verbalizzata) && "VERBALIZZATO".equalsIgnoreCase(stato)) {
                                if (insId != null) passedInsegnamentoIds.add(insId);
                                if (nomeIns != null) passedCoursesSet.add(nomeIns);
                            }
                        }
                    }
                }
            }
            model.addAttribute("prenotazioni", myBookings);
            model.addAttribute("bookedCallIds", bookedCallIds);

        } catch (Exception e) {
            model.addAttribute("error", "Errore nel caricamento delle prenotazioni: " + e.getMessage());
        }

        // 2. Carica il libretto universitario protetto dal Circuit Breaker e Fallback di Resilience4j
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
                    model.addAttribute("warning", "Il servizio carriere (student-service) è momentaneamente offline per manutenzione. I tuoi dati sono al sicuro.");
                    return new ArrayList<>();
                })
                .get();

        Map<String, GradeDTO> gradedCoursesMap = new HashMap<>();

        if (libretto != null) {
            for (GradeDTO grade : libretto) {
                gradedCoursesMap.put(grade.getNomeInsegnamento(), grade);
                if (grade.getVoto() != null && grade.getVoto() >= 18) {
                    passedCoursesSet.add(grade.getNomeInsegnamento());
                }
            }
        }
        model.addAttribute("gradedCoursesMap", gradedCoursesMap);
        model.addAttribute("passedCoursesSet", passedCoursesSet);

        // 3. Carica gli appelli d'esame ufficiali e FILTRA solo quelli APERTI e per materie NON SUPERATE
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Bearer " + token);
            HttpEntity<Void> requestEntity = new HttpEntity<>(headers);

            ResponseEntity<List<Map>> callsResponse = restTemplate.exchange(
                    examServiceUrl + "/calls",
                    HttpMethod.GET,
                    requestEntity,
                    new ParameterizedTypeReference<List<Map>>() {}
            );

            List<Map> allCalls = callsResponse.getBody();
            List<Map> availableCalls = new ArrayList<>();

            if (allCalls != null) {
                for (Map call : allCalls) {
                    Boolean chiuso = (Boolean) call.get("chiuso");
                    Map insegnamento = (Map) call.get("insegnamento");
                    Long insId = (insegnamento != null && insegnamento.get("id") != null) 
                            ? Long.valueOf(insegnamento.get("id").toString()) : null;
                    String nomeIns = (insegnamento != null) ? (String) insegnamento.get("nome") : null;

                    boolean appelloAperto = (chiuso == null || !chiuso);
                    boolean materiaSuperata = (insId != null && passedInsegnamentoIds.contains(insId)) 
                            || (nomeIns != null && passedCoursesSet.contains(nomeIns));

                    // Inseriamo nell'elenco disponibile SOLO se l'appello è aperto E la materia NON è superata
                    if (appelloAperto && !materiaSuperata) {
                        availableCalls.add(call);
                    }
                }
            }
            model.addAttribute("availableCalls", availableCalls);

        } catch (Exception e) {
            model.addAttribute("error", "Errore nel caricamento degli appelli: " + e.getMessage());
        }

        model.addAttribute("username", username);
        model.addAttribute("libretto", libretto);

        return "student-home";
    }

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

            String matricola = "M81001234"; 
            try {
                ResponseEntity<Map> infoResponse = restTemplate.exchange(
                        studentServiceUrl + "/me",
                        HttpMethod.GET,
                        requestEntity,
                        Map.class
                );
                if (infoResponse.getBody() != null && infoResponse.getBody().get("matricola") != null) {
                    matricola = (String) infoResponse.getBody().get("matricola");
                }
            } catch (Exception e) {
                System.err.println("⚠️ [StudentWebController] Impossibile contattare student-service per la matricola, uso profilatura di fallback.");
            }

            HttpHeaders bookHeaders = new HttpHeaders();
            bookHeaders.set("Authorization", "Bearer " + token);
            bookHeaders.set("X-Student-Matricola", matricola);
            HttpEntity<Void> bookEntity = new HttpEntity<>(bookHeaders);

            String bookEndpoint = examServiceUrl + "/calls/" + callId + "/book";
            ResponseEntity<Map> bookResponse = restTemplate.postForEntity(bookEndpoint, bookEntity, Map.class);

            if (bookResponse.getStatusCode().is2xxSuccessful() || bookResponse.getStatusCode().value() == 201) {
                model.addAttribute("success", "Prenotazione effettuata con successo!");
            } else {
                model.addAttribute("error", "Impossibile completare la prenotazione.");
            }

        } catch (org.springframework.web.client.HttpClientErrorException e) {
            model.addAttribute("error", "Impossibile prenotarsi: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            model.addAttribute("error", "Errore durante la prenotazione: " + e.getMessage());
        }

        return showStudentHome(session, model);
    }

    @PostMapping("/student/cancel-booking")
    public String cancelBooking(@RequestParam("bookingId") Long bookingId, HttpSession session, Model model) {
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

            String endpoint = examServiceUrl + "/bookings/" + bookingId + "/cancel";
            ResponseEntity<Map> response = restTemplate.postForEntity(endpoint, requestEntity, Map.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                model.addAttribute("success", "Prenotazione annullata con successo!");
            } else {
                model.addAttribute("error", "Impossibile annullare la prenotazione.");
            }
        } catch (org.springframework.web.client.HttpClientErrorException e) {
            model.addAttribute("error", "Impossibile annullare: " + e.getResponseBodyAsString());
        } catch (Exception e) {
            model.addAttribute("error", "Errore durante l'annullamento della prenotazione: " + e.getMessage());
        }

        return showStudentHome(session, model);
    }
}
