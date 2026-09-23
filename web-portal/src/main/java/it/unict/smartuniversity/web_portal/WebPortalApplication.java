
package it.unict.smartuniversity.web_portal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@SpringBootApplication
public class WebPortalApplication {

    public static void main(String[] args) {
        SpringApplication.run(WebPortalApplication.class, args);
    }

    /**
     * Registra RestTemplate con TIMEOUT espliciti di Connessione e Lettura (3000 ms).
     * Indispensabile affinché Resilience4J possa scattare ed attivare il Fallback
     * qualora un microservizio sia lento o bloccato.
     */
    @Bean
    public RestTemplate restTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(3000); // Timeout connessione socket: 3 sec
        factory.setReadTimeout(3000);    // Timeout lettura risposta HTTP: 3 sec
        return new RestTemplate(factory);
    }
}
