package it.unict.smartuniversity.examservice.controller;

import it.unict.smartuniversity.examservice.model.ExamCall;
import it.unict.smartuniversity.examservice.model.Insegnamento;
import it.unict.smartuniversity.examservice.model.Prenotazione;
import it.unict.smartuniversity.examservice.service.ExamService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    // --- INSEGNAMENTI ---
    @PostMapping("/courses")
    public ResponseEntity<?> createInsegnamento(@RequestBody Insegnamento insegnamento, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso negato! Solo gli amministratori possono inserire insegnamenti."));
        }
        try {
            Insegnamento created = examService.createInsegnamento(insegnamento);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/courses")
    public ResponseEntity<?> getAllCourses() {
        return ResponseEntity.ok(examService.getAllInsegnamenti());
    }

    @GetMapping("/courses/my")
    public ResponseEntity<?> getMyCourses(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return ResponseEntity.ok(examService.getInsegnamentiByDocente(username));
    }

    // --- APPELLI ---
    @PostMapping("/calls")
    public ResponseEntity<?> createExamCall(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");

        if (!"DOCENTE".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso negato! Solo i docenti possono creare appelli d'esame."));
        }

        try {
            Long insegnamentoId = null;
            if (payload.get("insegnamentoId") != null) {
                insegnamentoId = Long.valueOf(payload.get("insegnamentoId").toString());
            }

            String dataEsameStr = (String) payload.get("dataEsame");
            if (insegnamentoId == null || dataEsameStr == null) {
                return ResponseEntity.badRequest()
                        .body(Map.of("error", "Parametri mancanti: insegnamentoId o dataEsame"));
            }

            LocalDate dataEsame = LocalDate.parse(dataEsameStr);
            ExamCall created = examService.createExamCall(insegnamentoId, dataEsame, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/calls")
    public ResponseEntity<?> getAllCalls() {
        return ResponseEntity.ok(examService.getAllExamCalls());
    }

    @PostMapping("/calls/{id}/close")
    public ResponseEntity<?> closeExamCall(@PathVariable("id") Long callId, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");

        if (!"DOCENTE".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Accesso negato!"));
        }

        try {
            ExamCall closedCall = examService.closeExamCall(callId, username);
            return ResponseEntity.ok(closedCall);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    // --- PRENOTAZIONI ---
    @PostMapping("/calls/{id}/book")
    public ResponseEntity<?> bookExamCall(@PathVariable("id") Long callId, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");

        // Legge l'header X-Student-Matricola con fallback su parametro query
        String matricola = request.getHeader("X-Student-Matricola");
        if (matricola == null || matricola.isBlank()) {
            matricola = request.getParameter("matricola");
        }

        if (!"STUDENTE".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso riservato agli studenti!"));
        }

        try {
            Prenotazione prenotazione = examService.bookExamCall(callId, matricola, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(prenotazione);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }
    
    @GetMapping("/docente/pending-bookings")
    public ResponseEntity<?> getPendingBookings(HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");

        if (!"DOCENTE".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(Map.of("error", "Accesso negato!"));
        }
        return ResponseEntity.ok(examService.getPendingBookingsForDocente(username));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<?> getMyBookings(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return ResponseEntity.ok(examService.getBookingsByUsername(username));
    }

    // --- VERBALIZZAZIONE ---
    @PostMapping("/grades")
    public ResponseEntity<?> recordGrade(@RequestBody Map<String, Object> payload, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");

        if (!"DOCENTE".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso negato! Solo i docenti possono verbalizzare i voti."));
        }

        try {
            if (payload.get("bookingId") == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "Parametro 'bookingId' mancante."));
            }

            Long bookingId = Long.valueOf(payload.get("bookingId").toString());
            String esito = payload.get("esito") != null ? payload.get("esito").toString() : "PROMOSSO";

            Integer voto = 0;
            Boolean lode = false;

            if ("PROMOSSO".equalsIgnoreCase(esito)) {
                if (payload.get("voto") == null) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Selezionare un voto valido per la promozione."));
                }
                voto = Integer.valueOf(payload.get("voto").toString());
                if (voto < 18 || voto > 30) {
                    return ResponseEntity.badRequest()
                            .body(Map.of("error", "Il voto di promozione deve essere compreso tra 18 e 30."));
                }
                if (payload.get("lode") != null) {
                    lode = Boolean.valueOf(payload.get("lode").toString());
                }
            }

            examService.recordGrade(bookingId, voto, lode, username);
            return ResponseEntity
                    .ok(Map.of("message", "Verbalizzazione registrata ed inviata a RabbitMQ con successo!"));

        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/bookings/{id}/cancel")
    public ResponseEntity<?> cancelBooking(@PathVariable("id") Long bookingId, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");

        if (!"STUDENTE".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso riservato agli studenti!"));
        }

        try {
            examService.cancelBooking(bookingId, username);
            return ResponseEntity.ok(Map.of("message", "Prenotazione annullata con successo!"));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

}
