package it.unict.smartuniversity.web_portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class WebPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebPortalApplication.class, args);
    }

    /**
     * Registriamo RestTemplate come Bean nel contesto di Spring.
     * RestTemplate è il client HTTP sincrono che il portale utilizzerà 
     * per inoltrare le richieste (es. Login o Recupero Voti) ai microservizi di backend.
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}