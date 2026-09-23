package it.unict.smartuniversity.auth_service.service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import it.unict.smartuniversity.auth_service.model.Role;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
public class JwtService {

    // Chiave segreta utilizzata per firmare il token (tienila al sicuro!)
    private static final String SECRET_KEY = "SmartUniversitySuperSecretKeyForJWT";
    private static final String ISSUER = "auth-service";
    
    // Il token scadrà dopo 1 ora (3600 secondi)
    private static final long EXPIRATION_TIME = 3600_000; 

    private final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);

    /**
     * Genera un JWT Token firmato contenente username e ruolo dell'utente.
     */
    public String generateToken(String username, Role role) {
        return JWT.create()
                .withIssuer(ISSUER) // Chi emette il token (iss)
                .withSubject(username) // L'utente identificato (sub)
                .withClaim("role", role.name()) // Ruolo dell'utente per il controllo RBAC
                .withIssuedAt(new Date()) // Data di generazione (iat)
                .withExpiresAt(new Date(System.currentTimeMillis() + EXPIRATION_TIME)) // Scadenza (exp)
                .sign(algorithm); // Firma crittografica del token
    }

    /**
     * Verifica la validità di un JWT Token.
     * Lancia un'eccezione se il token è alterato o scaduto.
     */
    public DecodedJWT verifyToken(String token) {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token); // Esegue il controllo crittografico
    }
}