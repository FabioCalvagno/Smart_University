package it.unict.smartuniversity.student_service;

import it.unict.smartuniversity.student_service.security.JwtInterceptor;
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
        // Applica il Reference Monitor (JwtInterceptor) a tutte le rotte che iniziano con /api/
        registry.addInterceptor(jwtInterceptor)
                .addPathPatterns("/api/**");
    }
}