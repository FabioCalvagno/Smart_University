package it.unict.smartuniversity.student_service.listener;

import it.unict.smartuniversity.student_service.dto.GradeEvent;
import it.unict.smartuniversity.student_service.model.Grade;
import it.unict.smartuniversity.student_service.model.Student;
import it.unict.smartuniversity.student_service.repository.GradeRepository;
import it.unict.smartuniversity.student_service.repository.StudentRepository;
import it.unict.smartuniversity.student_service.security.StudentGradeCacheAspect; // IMPORTANTE PER LA CACHE
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class GradeListener {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GradeRepository gradeRepository;

    @RabbitListener(queues = "student-grade-queue")
    public void handleGradeRecorded(GradeEvent event) {
        System.out.println("====== CONSUMER: Ricevuto nuovo evento voto da RabbitMQ! ======");
        System.out.println("Esame: " + event.getNomeInsegnamento() + " | Voto: " + event.getVoto() + " | Studente: " + event.getMatricola());

        try {
            Student student = studentRepository.findAll().stream()
                    .filter(s -> event.getMatricola().equals(s.getMatricola()))
                    .findFirst()
                    .orElseThrow(() -> new RuntimeException("Studente non trovato per la matricola: " + event.getMatricola()));
            
            boolean giaRegistrato = gradeRepository.existsByStudentAndNomeInsegnamento(student, event.getNomeInsegnamento());
            
            if (giaRegistrato) {
                System.out.println("====== CONSUMER: [Idempotent Receiver] Evento duplicato rilevato. Voto già registrato per " + event.getNomeInsegnamento() + ". ======");
                return;
            }

            Grade grade = new Grade();
            grade.setNomeInsegnamento(event.getNomeInsegnamento());
            grade.setVoto(event.getVoto());
            grade.setLode(event.isLode());
            grade.setDocente(event.getDocente());
            grade.setStudent(student);

            gradeRepository.save(grade);
            System.out.println("====== CONSUMER: Carriera aggiornata con successo nel Database! ======");

            // --- ⚡ INVALIDS (EVICT) LA CACHE DELL'UTENTE INTERCETTATA DALL'ASPETTO ---
            StudentGradeCacheAspect.evictCacheForUser(student.getUsername());

        } catch (Exception e) {
            System.err.println("❌ Errore durante l'elaborazione asincrona del voto: " + e.getMessage());
        }
    }
}