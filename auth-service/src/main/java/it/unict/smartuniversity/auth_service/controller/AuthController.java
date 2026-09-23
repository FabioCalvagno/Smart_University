package it.unict.smartuniversity.auth_service.controller;

import it.unict.smartuniversity.auth_service.model.User;
import it.unict.smartuniversity.auth_service.model.Role;
import it.unict.smartuniversity.auth_service.service.AuthService;
import it.unict.smartuniversity.auth_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private AuthService authService;

    @Autowired
    private UserRepository userRepository;

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

    /**
     * NUOVO: Restituisce il ruolo di uno specifico utente per la verifica dei permessi.
     */
    @GetMapping("/users/{username}/role")
    public ResponseEntity<?> getUserRole(@PathVariable("username") String username) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isPresent()) {
            return ResponseEntity.ok(Map.of(
                "username", userOpt.get().getUsername(),
                "role", userOpt.get().getRole().name()
            ));
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of("error", "Utente non trovato nel server d'autenticazione."));
        }
    }
}
