package it.unict.smartuniversity.student_service.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String QUEUE_NAME = "student-grade-queue";

    // Questo Bean dice a Spring di creare automaticamente la coda 
    // su RabbitMQ se non esiste già al momento dell'avvio!
    @Bean
    public Queue studentGradeQueue() {
        return new Queue(QUEUE_NAME, true); // durable = true (sopravvive al riavvio del broker)
    }
    
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}