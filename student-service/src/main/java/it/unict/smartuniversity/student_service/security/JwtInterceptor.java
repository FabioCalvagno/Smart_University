package it.unict.smartuniversity.student_service.security;

import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class JwtInterceptor implements HandlerInterceptor {

    @Autowired
    private JwtValidator jwtValidator;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        // 1. Permetti le richieste di pre-flight CORS (opzionali per il browser)
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // 2. Estrai l'header di Autorizzazione
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token mancante o non valido! Richiesto Bearer Token.\"}");
            response.setContentType("application/json");
            return false; // Blocca la richiesta (il Reference Monitor nega l'accesso)
        }

        // Estrae il token effettivo saltando i primi 7 caratteri ("Bearer ")
        String token = authHeader.substring(7);

        try {
            // 3. Valida il token crittograficamente
            DecodedJWT jwt = jwtValidator.validateAndDecode(token);

            // 4. Salva i dati estratti dal token nella richiesta, così il controller potrà usarli
            request.setAttribute("username", jwt.getSubject());
            request.setAttribute("role", jwt.getClaim("role").asString());

            return true; // Token valido, inoltra la richiesta al Controller protetto
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token non valido o scaduto: " + e.getMessage() + "\"}");
            response.setContentType("application/json");
            return false; // Blocca la richiesta
        }
    }
}