package it.unict.smartuniversity.student_service.listener;

import it.unict.smartuniversity.student_service.dto.GradeEvent;
import it.unict.smartuniversity.student_service.service.StudentService;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GradeListener {

    @Autowired
    private StudentService studentService;

    @RabbitListener(queues = "student-grade-queue")
    public void handleGradeRecorded(GradeEvent event) {
        System.out.println("====== CONSUMER: Ricevuto nuovo evento voto da RabbitMQ! ======");
        System.out.println("Esame: " + event.getNomeInsegnamento() + " | Voto: " + event.getVoto() + " | Studente: " + event.getMatricola());

        try {
            // Delega a StudentService che scarta i voti < 18 (Rimandato) ed applica l'Idempotent Receiver
            studentService.saveGradeFromEvent(event);
            System.out.println("====== CONSUMER: Elaborazione evento completata! ======");
        } catch (Exception e) {
            System.err.println("❌ Errore durante l'elaborazione asincrona del voto: " + e.getMessage());
        }
    }
}
