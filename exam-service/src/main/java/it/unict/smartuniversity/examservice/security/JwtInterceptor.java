package it.unict.smartuniversity.examservice.security;

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
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token mancante o non valido! Richiesto Bearer Token.\"}");
            response.setContentType("application/json");
            return false;
        }

        String token = authHeader.substring(7);
        try {
            DecodedJWT jwt = jwtValidator.validateAndDecode(token);
            request.setAttribute("username", jwt.getSubject());
            request.setAttribute("role", jwt.getClaim("role").asString());
            return true;
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.getWriter().write("{\"error\": \"Token non valido o scaduto: " + e.getMessage() + "\"}");
            response.setContentType("application/json");
            return false;
        }
    }
}