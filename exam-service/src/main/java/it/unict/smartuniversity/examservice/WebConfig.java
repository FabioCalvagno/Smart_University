package it.unict.smartuniversity.examservice;

import it.unict.smartuniversity.examservice.security.JwtInterceptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Autowired
    private JwtInterceptor jwtInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // Applica la sicurezza a tutti gli endpoint /api/
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**");
    }
}