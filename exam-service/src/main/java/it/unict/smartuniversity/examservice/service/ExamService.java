package it.unict.smartuniversity.examservice.service;

import it.unict.smartuniversity.examservice.config.RabbitMQConfig;
import it.unict.smartuniversity.examservice.dto.GradeEvent;
import it.unict.smartuniversity.examservice.model.ExamCall;
import it.unict.smartuniversity.examservice.model.Insegnamento;
import it.unict.smartuniversity.examservice.model.Prenotazione;
import it.unict.smartuniversity.examservice.repository.ExamCallRepository;
import it.unict.smartuniversity.examservice.repository.InsegnamentoRepository;
import it.unict.smartuniversity.examservice.repository.PrenotazioneRepository;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ExamService {

    @Autowired
    private ExamCallRepository examCallRepository;

    @Autowired
    private InsegnamentoRepository insegnamentoRepository;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // --- SEZIONE INSEGNAMENTI (Admin Only) ---
    public Insegnamento createInsegnamento(Insegnamento insegnamento) {
        if (insegnamentoRepository.findByCodice(insegnamento.getCodice()).isPresent()) {
            throw new RuntimeException("Insegnamento con questo codice già esistente!");
        }
        return insegnamentoRepository.save(insegnamento);
    }

    public List<Insegnamento> getAllInsegnamenti() {
        return insegnamentoRepository.findAll();
    }

    public List<Insegnamento> getInsegnamentiByDocente(String docente) {
        return insegnamentoRepository.findByDocente(docente);
    }

    // --- SEZIONE APPELLI (Docente Only) ---
    public ExamCall createExamCall(ExamCall call, String loggedInUsername) {
        Insegnamento insegnamento = insegnamentoRepository.findById(call.getInsegnamento().getId())
                .orElseThrow(() -> new RuntimeException("Insegnamento associato non trovato!"));
        
        // Verifica di sicurezza: il docente può creare appelli solo per le proprie materie
        if (!insegnamento.getDocente().equalsIgnoreCase(loggedInUsername) && !loggedInUsername.equalsIgnoreCase("admin")) {
            throw new RuntimeException("Non sei autorizzato a creare appelli d'esame per questa materia!");
        }
        
        call.setInsegnamento(insegnamento);
        call.setDocente(insegnamento.getDocente());
        return examCallRepository.save(call);
    }

    public List<ExamCall> getAllExamCalls() {
        return examCallRepository.findAll();
    }

    // --- SEZIONE PRENOTAZIONI (Studente Only) ---
    public Prenotazione bookExamCall(Long callId, String matricola, String username) {
        ExamCall call = examCallRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Appello d'esame non trovato!"));

        if (prenotazioneRepository.findByExamCallAndMatricola(call, matricola).isPresent()) {
            throw new RuntimeException("Risulti già prenotato a questo appello!");
        }

        Prenotazione prenotazione = new Prenotazione();
        prenotazione.setExamCall(call);
        prenotazione.setMatricola(matricola);
        prenotazione.setUsername(username);
        return prenotazioneRepository.save(prenotazione);
    }

    public List<Prenotazione> getBookingsByCallId(Long callId) {
        ExamCall call = examCallRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Appello d'esame non trovato!"));
        return prenotazioneRepository.findByExamCall(call);
    }

    public List<Prenotazione> getBookingsByUsername(String username) {
        return prenotazioneRepository.findByUsername(username);
    }

    // --- SEZIONE VERBALIZZAZIONE (RabbitMQ) ---
    public void recordGrade(GradeEvent gradeEvent, String docenteUsername) {
        gradeEvent.setDocente(docenteUsername);

        System.out.println("====== PRODUCER: Registrazione voto per matricola: " + gradeEvent.getMatricola() + " ======");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                gradeEvent
        );

        System.out.println("====== PRODUCER: Messaggio pubblicato con successo su RabbitMQ! ======");
    }
}