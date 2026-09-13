package it.unict.smartuniversity.auth_service.service;

import com.lambdaworks.crypto.SCryptUtil;
import it.unict.smartuniversity.auth_service.model.User;
import it.unict.smartuniversity.auth_service.model.Role;
import it.unict.smartuniversity.auth_service.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class AuthService {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private JwtService jwtService; // Iniettiamo il servizio JWT

    /**
     * Registra un nuovo utente cifrando la password tramite Scrypt.
     */
    public User register(String username, String password, Role role) {
        if (userRepository.findByUsername(username).isPresent()) {
            throw new RuntimeException("Username già esistente!");
        }

        String passwordHash = SCryptUtil.scrypt(password, 32768, 8, 1);

        User newUser = new User();
        newUser.setUsername(username);
        newUser.setPassword(passwordHash);
        newUser.setRole(role);

        return userRepository.save(newUser);
    }

    /**
     * Effettua il login dell'utente. Se le credenziali sono corrette,
     * genera e restituisce un Token JWT (Proof of Identity).
     */
    public String login(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        
        if (userOpt.isPresent() && SCryptUtil.check(password, userOpt.get().getPassword())) {
            // Se le credenziali sono corrette, generiamo il Token JWT
            return jwtService.generateToken(username, userOpt.get().getRole());
        }
        
        throw new RuntimeException("Credenziali errate!");
    }

    /**
     * Verifica le credenziali dell'utente (usato per debug o test veloci).
     */
    public boolean verifyCredentials(String username, String password) {
        Optional<User> userOpt = userRepository.findByUsername(username);
        if (userOpt.isEmpty()) {
            return false;
        }
        return SCryptUtil.check(password, userOpt.get().getPassword());
    }
}