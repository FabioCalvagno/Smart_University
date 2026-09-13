package it.unict.smartuniversity.auth_service.controller;

import it.unict.smartuniversity.auth_service.model.User;
import it.unict.smartuniversity.auth_service.model.Role;
import it.unict.smartuniversity.auth_service.service.AuthService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    /**
     * Endpoint per registrare un nuovo utente.
     * Riceve un JSON con username, password e ruolo.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");
            
            // Converte la stringa del ruolo in Enum (STUDENTE, DOCENTE, ADMIN)
            Role role = Role.valueOf(request.get("role").toUpperCase());

            User registeredUser = authService.register(username, password, role);
            
            return ResponseEntity.ok(Map.of(
                "message", "Utente registrato con successo!",
                "id", registeredUser.getId(),
                "username", registeredUser.getUsername(),
                "role", registeredUser.getRole()
            ));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", "Ruolo non valido! Usa STUDENTE, DOCENTE o ADMIN."));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * Endpoint ufficiale per il login.
     * Restituisce il token JWT se le credenziali sono corrette.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> request) {
        try {
            String username = request.get("username");
            String password = request.get("password");

            String token = authService.login(username, password);
            return ResponseEntity.ok(Map.of(
                "message", "Login effettuato con successo!",
                "token", token
            ));
        } catch (Exception e) {
            return ResponseEntity.status(401).body(Map.of("error", e.getMessage()));
        }
    }
}