package it.unict.smartuniversity.student_service.service;

import it.unict.smartuniversity.student_service.model.Student;
import it.unict.smartuniversity.student_service.repository.StudentRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import it.unict.smartuniversity.student_service.dto.GradeDTO;
import it.unict.smartuniversity.student_service.model.Grade;
import it.unict.smartuniversity.student_service.repository.GradeRepository;
import org.springframework.transaction.annotation.Transactional;
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
        // 1. Cerca lo studente per username
        Student student = studentRepository.findByUsername(username)
                .orElseThrow(() -> new RuntimeException("Studente non trovato per l'username: " + username));

        // 2. Cerca tutti i voti associati a questo studente nel database
        List<Grade> grades = gradeRepository.findByStudent(student);

        // 3. Converte la lista di Entity in DTO (Pattern Assembler)
        return grades.stream()
                .map(grade -> new GradeDTO(
                        grade.getNomeInsegnamento(),
                        grade.getVoto(),
                        grade.isLode(), // <--- Cambiato da getLode() a isLode()
                        grade.getDocente()
                ))
                .collect(Collectors.toList());
    }
}