package it.unict.smartuniversity.student_service.controller;

import it.unict.smartuniversity.student_service.dto.GradeDTO;
import it.unict.smartuniversity.student_service.service.StudentService;
import it.unict.smartuniversity.student_service.security.JwtValidator;
import it.unict.smartuniversity.student_service.model.Student;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/students")
public class StudentController {

    @Autowired
    private StudentService studentService;

    @Autowired
    private JwtValidator jwtValidator;

    @PostMapping
    public ResponseEntity<?> createStudent(@RequestBody Student student, HttpServletRequest request) {
        String role = (String) request.getAttribute("role");
        if (!"ADMIN".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso negato! Solo gli amministratori possono registrare nuove anagrafiche."));
        }
        try {
            Student created = studentService.createStudent(student);
            return ResponseEntity.status(HttpStatus.CREATED).body(created);
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * NUOVO: Restituisce il profilo anagrafico dello studente attualmente loggato.
     * Necessario per recuperare la matricola ed effettuare le prenotazioni in modo dinamico.
     */
    @GetMapping("/me")
    public ResponseEntity<?> getMyProfile(HttpServletRequest request) {
        String username = (String) request.getAttribute("username");
        String role = (String) request.getAttribute("role");

        if (!"STUDENTE".equalsIgnoreCase(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(Map.of("error", "Accesso riservato agli studenti!"));
        }

        try {
            Student student = studentService.getStudentByUsername(username);
            return ResponseEntity.ok(student);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/me/grades")
    public ResponseEntity<?> getMyGrades(@RequestHeader("Authorization") String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token mancante o non valido!"));
        }
        String token = authorizationHeader.substring(7);
        try {
            DecodedJWT decodedJWT = jwtValidator.validateAndDecode(token);
            String role = decodedJWT.getClaim("role").asString();
            if (role == null || !"STUDENTE".equalsIgnoreCase(role)) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(Map.of("error", "Accesso negato!"));
            }
            String username = decodedJWT.getSubject();
            List<GradeDTO> libretto = studentService.getLibrettoByUsername(username);
            return ResponseEntity.ok(libretto);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(Map.of("error", "Token scaduto! " + e.getMessage()));
        }
    }
}