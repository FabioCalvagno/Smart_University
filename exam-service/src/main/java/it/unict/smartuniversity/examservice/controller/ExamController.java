package it.unict.smartuniversity.examservice.controller;

import it.unict.smartuniversity.examservice.dto.GradeEvent;
import it.unict.smartuniversity.examservice.model.ExamCall;
import it.unict.smartuniversity.examservice.model.Insegnamento;
import it.unict.smartuniversity.examservice.model.Prenotazione;
import it.unict.smartuniversity.examservice.service.ExamService;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/exams")
public class ExamController {

    @Autowired
    private ExamService examService;

    // --- ENDPOINT INSEGNAMENTI (Admin) ---
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

    // --- ENDPOINT APPELLI (Docente) ---
    @PostMapping("/calls")
    public ResponseEntity<?> createExamCall(@RequestBody ExamCall examCall, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");

        if (!"DOCENTE".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso negato! Solo i docenti possono creare appelli d'esame."));
        }

        try {
            ExamCall created = examService.createExamCall(examCall, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/calls")
    public ResponseEntity<?> getAllCalls() {
        return ResponseEntity.ok(examService.getAllExamCalls());
    }

    // --- ENDPOINT PRENOTAZIONI (Studente) ---
    @PostMapping("/calls/{id}/book")
    public ResponseEntity<?> bookExamCall(@PathVariable("id") Long callId, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");
        String matricola = request.getHeader("X-Student-Matricola");

        if (!"STUDENTE".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso negato! Solo gli studenti possono prenotarsi agli appelli."));
        }

        try {
            Prenotazione prenotazione = examService.bookExamCall(callId, matricola, username);
            return ResponseEntity.status(HttpStatus.CREATED).body(prenotazione);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/calls/{id}/bookings")
    public ResponseEntity<?> getBookings(@PathVariable("id") Long callId, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"DOCENTE".equalsIgnoreCase(role) && !"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso negato!"));
        }
        return ResponseEntity.ok(examService.getBookingsByCallId(callId));
    }

    @GetMapping("/my-bookings")
    public ResponseEntity<?> getMyBookings(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        return ResponseEntity.ok(examService.getBookingsByUsername(username));
    }

    // --- ENDPOINT VERBALIZZAZIONE (Docente) ---
    @PostMapping("/grades")
    public ResponseEntity<?> recordGrade(@RequestBody GradeEvent gradeEvent, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        String username = (String) request.getAttribute("username");

        if (!"DOCENTE".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso negato! Solo i docenti possono verbalizzare i voti."));
        }

        try {
            examService.recordGrade(gradeEvent, username);
            return ResponseEntity.ok(Map.of("message", "Voto inviato per la verbalizzazione asincrona con successo!"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Errore durante l'invio del messaggio: " + e.getMessage()));
        }
    }
}