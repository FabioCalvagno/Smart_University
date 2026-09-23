package it.unict.smartuniversity.examservice.security;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.JWTVerifier;
import org.springframework.stereotype.Component;

@Component
public class JwtValidator {
    private static final String SECRET_KEY = "SmartUniversitySuperSecretKeyForJWT";
    private static final String ISSUER = "auth-service";
    private final Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY);

    public DecodedJWT validateAndDecode(String token) {
        JWTVerifier verifier = JWT.require(algorithm)
                .withIssuer(ISSUER)
                .build();
        return verifier.verify(token);
    }
}