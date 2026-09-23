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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ExamService {

    @Autowired
    private InsegnamentoRepository insegnamentoRepository;

    @Autowired
    private ExamCallRepository examCallRepository;

    @Autowired
    private PrenotazioneRepository prenotazioneRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    // --- INSEGNAMENTI ---
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

    // --- APPELLI ---
    public ExamCall createExamCall(Long insegnamentoId, LocalDate dataEsame, String loggedInUsername) {
        Insegnamento insegnamento = insegnamentoRepository.findById(insegnamentoId)
                .orElseThrow(() -> new RuntimeException("Insegnamento non trovato (ID: " + insegnamentoId + ")"));

        String docenteInsegnamento = insegnamento.getDocente();
        if (docenteInsegnamento != null && !docenteInsegnamento.equalsIgnoreCase(loggedInUsername) && !"admin".equalsIgnoreCase(loggedInUsername)) {
            throw new RuntimeException("Non sei autorizzato a creare appelli per questa materia!");
        }

        ExamCall call = new ExamCall();
        call.setInsegnamento(insegnamento);
        call.setDataEsame(dataEsame);
        call.setDocente(loggedInUsername);
        call.setChiuso(false);

        return examCallRepository.save(call);
    }

    public List<ExamCall> getAllExamCalls() {
        return examCallRepository.findAll();
    }

    public ExamCall closeExamCall(Long callId, String loggedInUsername) {
        ExamCall call = examCallRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Appello non trovato!"));
        call.setChiuso(true);
        return examCallRepository.save(call);
    }

    // --- PRENOTAZIONI ---
    public Prenotazione bookExamCall(Long callId, String matricola, String username) {
        ExamCall call = examCallRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Appello d'esame non trovato!"));

        if (call.isChiuso()) {
            throw new RuntimeException("Impossibile prenotarsi: le iscrizioni per questo appello sono state chiuse dal docente!");
        }

        if (prenotazioneRepository.findByExamCallAndMatricola(call, matricola).isPresent()) {
            throw new RuntimeException("Risulti già prenotato a questo specifico appello!");
        }

        List<Prenotazione> userBookings = prenotazioneRepository.findByUsername(username);

        // CONTROLLO 1: L'esame è già stato superato e verbalizzato?
        boolean giaSuperato = userBookings.stream()
                .anyMatch(b -> b.isVerbalizzata() 
                        && "VERBALIZZATO".equalsIgnoreCase(b.getStato()) 
                        && b.getExamCall() != null 
                        && b.getExamCall().getInsegnamento() != null 
                        && b.getExamCall().getInsegnamento().getId().equals(call.getInsegnamento().getId()));

        if (giaSuperato) {
            throw new RuntimeException("Hai già superato e verbalizzato l'insegnamento '" 
                    + call.getInsegnamento().getNome() + "'! Non puoi prenotarti a nuovi appelli per questa materia.");
        }

        // CONTROLLO 2: C'è una prenotazione PENDENTE per la stessa materia
        boolean prenotazionePendente = userBookings.stream()
                .filter(b -> !b.isVerbalizzata())
                .anyMatch(b -> b.getExamCall() != null 
                        && b.getExamCall().getInsegnamento() != null 
                        && b.getExamCall().getInsegnamento().getId().equals(call.getInsegnamento().getId()));

        if (prenotazionePendente) {
            throw new RuntimeException("Sei già prenotato a un appello attivo per l'insegnamento '" 
                    + call.getInsegnamento().getNome() + "'. Cancella la precedente prenotazione prima di sceglierne un'altra!");
        }

        Prenotazione prenotazione = new Prenotazione();
        prenotazione.setExamCall(call);
        prenotazione.setMatricola(matricola);
        prenotazione.setUsername(username);
        prenotazione.setStato("ATTESA_VERBALIZZAZIONE");

        return prenotazioneRepository.save(prenotazione);
    }

    public List<Prenotazione> getBookingsByUsername(String username) {
        return prenotazioneRepository.findByUsername(username);
    }

    public List<Prenotazione> getBookingsByCallId(Long callId) {
        ExamCall call = examCallRepository.findById(callId)
                .orElseThrow(() -> new RuntimeException("Appello d'esame non trovato!"));
        return prenotazioneRepository.findByExamCall(call);
    }

    /**
     * Restituisce unicamente le prenotazioni attive e NON ancora verbalizzate per i corsi del docente
     */
    public List<Prenotazione> getPendingBookingsForDocente(String docenteUsername) {
        return prenotazioneRepository.findAll().stream()
                .filter(p -> !p.isVerbalizzata())
                .filter(p -> p.getExamCall() != null 
                        && p.getExamCall().getInsegnamento() != null 
                        && p.getExamCall().getDocente() != null 
                        && (p.getExamCall().getDocente().equalsIgnoreCase(docenteUsername) || "admin".equalsIgnoreCase(docenteUsername)))
                .collect(Collectors.toList());
    }

    public void cancelBooking(Long bookingId, String username) {
        Prenotazione prenotazione = prenotazioneRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata!"));

        if (!prenotazione.getUsername().equalsIgnoreCase(username)) {
            throw new RuntimeException("Non sei autorizzato a cancellare questa prenotazione!");
        }

        if (prenotazione.getExamCall().isChiuso()) {
            throw new RuntimeException("Impossibile cancellare: le iscrizioni a questo appello sono state chiuse dal docente!");
        }

        prenotazioneRepository.delete(prenotazione);
    }

    // --- VERBALIZZAZIONE (RabbitMQ) ---
    @Transactional
    public void recordGrade(Long bookingId, Integer voto, Boolean lode, String docenteUsername) {
        Prenotazione booking = prenotazioneRepository.findById(bookingId)
                .orElseThrow(() -> new RuntimeException("Prenotazione non trovata (ID: " + bookingId + ")"));

        if (booking.isVerbalizzata()) {
            throw new RuntimeException("Questa prenotazione è già stata verbalizzata in precedenza!");
        }

        boolean promosso = (voto != null && voto >= 18);

        booking.setVerbalizzata(true);
        booking.setStato(promosso ? "VERBALIZZATO" : "RIMANDATO");
        prenotazioneRepository.save(booking);

        GradeEvent gradeEvent = new GradeEvent();
        gradeEvent.setMatricola(booking.getMatricola());
        gradeEvent.setNomeInsegnamento(booking.getExamCall().getInsegnamento().getNome());
        gradeEvent.setVoto(promosso ? voto : 0);
        gradeEvent.setLode(promosso && voto == 30 && Boolean.TRUE.equals(lode));
        gradeEvent.setDocente(docenteUsername);

        System.out.println("====== PRODUCER: Verbalizzazione esame (" + (promosso ? voto : "RIMANDATO") + ") per matricola: " + booking.getMatricola() + " ======");

        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EXCHANGE_NAME,
                RabbitMQConfig.ROUTING_KEY,
                gradeEvent
        );
    }
}
