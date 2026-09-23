package it.unict.smartuniversity.student_service.service;

import it.unict.smartuniversity.student_service.dto.GradeDTO;
import it.unict.smartuniversity.student_service.dto.GradeEvent;
import it.unict.smartuniversity.student_service.model.Grade;
import it.unict.smartuniversity.student_service.model.Student;
import it.unict.smartuniversity.student_service.repository.GradeRepository;
import it.unict.smartuniversity.student_service.repository.StudentRepository;
import it.unict.smartuniversity.student_service.security.StudentGradeCacheAspect;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class StudentService {

    @Autowired
    private StudentRepository studentRepository;

    @Autowired
    private GradeRepository gradeRepository;

    public Student createStudent(Student student) {
        if (studentRepository.findByUsername(student.getUsername()).isPresent()) {
            throw new RuntimeException("Uno studente con questo username esiste già!");
        }
        return studentRepository.save(student);
    }

    public Student getStudentByUsername(String username) {
        return studentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Studente non trovato per l'username: " + username));
    }

    public List<Student> getAllStudents() {
        return studentRepository.findAll();
    }

    @Transactional(readOnly = true)
    public List<GradeDTO> getLibrettoByUsername(String username) {
        Student student = studentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Studente non trovato per l'username: " + username));

        List<Grade> grades = gradeRepository.findByStudent(student);

        return grades.stream()
                .map(grade -> new GradeDTO(
                        grade.getNomeInsegnamento(),
                        grade.getVoto(),
                        grade.isLode(),
                        grade.getDocente()
                ))
                .collect(Collectors.toList());
    }

    @Transactional
    public void saveGradeFromEvent(GradeEvent event) {
        // Se l'esame è "RIMANDATO" (voto < 18), ignoriamo il salvataggio nel libretto
        if (event.getVoto() < 18) {
            System.out.println("ℹ️ [StudentService] Esito 'RIMANDATO' per la matricola " 
                    + event.getMatricola() + " (" + event.getNomeInsegnamento() + "). Nessun voto aggiunto al libretto.");
            
            studentRepository.findByMatricola(event.getMatricola())
                    .ifPresent(s -> StudentGradeCacheAspect.evictCacheForUser(s.getUsername()));
            return;
        }

        Student student = studentRepository.findByMatricola(event.getMatricola())
                .orElseThrow(() -> new RuntimeException("Studente non trovato per matricola: " + event.getMatricola()));

        if (gradeRepository.existsByStudentAndNomeInsegnamento(student, event.getNomeInsegnamento())) {
            System.out.println("⚠️ [Idempotent Receiver] Esame '" + event.getNomeInsegnamento() + "' già presente nel libretto.");
            return;
        }

        Grade grade = new Grade();
        grade.setStudent(student);
        grade.setNomeInsegnamento(event.getNomeInsegnamento());
        grade.setVoto(event.getVoto());
        grade.setLode(event.isLode());
        grade.setDocente(event.getDocente());

        gradeRepository.save(grade);

        StudentGradeCacheAspect.evictCacheForUser(student.getUsername());
    }
}
