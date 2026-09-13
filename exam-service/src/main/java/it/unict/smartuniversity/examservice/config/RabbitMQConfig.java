package it.unict.smartuniversity.examservice.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // Nomi dei componenti fisici su RabbitMQ
    public static final String EXCHANGE_NAME = "exam-exchange";
    public static final String QUEUE_NAME = "student-grade-queue";
    public static final String ROUTING_KEY = "exam.grade.recorded";

    // 1. Dichiarazione dello Scambio Diretto (Direct Exchange)
    @Bean
    public DirectExchange examExchange() {
        return new DirectExchange(EXCHANGE_NAME, true, false); // durable, autoDelete
    }

    // 2. Dichiarazione della Coda che riceverà i voti da registrare
    @Bean
    public Queue studentGradeQueue() {
        return new Queue(QUEUE_NAME, true); // durable (sopravvive ai crash del broker)
    }

    // 3. Creazione del collegamento (Binding) tra Coda ed Exchange tramite Routing Key
    @Bean
    public Binding binding(Queue studentGradeQueue, DirectExchange examExchange) {
        return BindingBuilder
                .bind(studentGradeQueue)
                .to(examExchange)
                .with(ROUTING_KEY);
    }

    // 4. Configurazione del serializzatore JSON.
    // Di default Spring AMQP userebbe la serializzazione nativa di Java (sconsigliata).
    // Con questo bean, i nostri messaggi viaggeranno su RabbitMQ formattati in JSON.
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}