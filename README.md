# 🎓 Smart University - Guida all'Avvio del Progetto

Progetto per l'insegnamento di **Ingegneria dei Sistemi Distribuiti**  
**Corso di Laurea Magistrale in Informatica** - Università degli Studi di Catania  
**Candidato:** Fabio Calvagno  
**Docente:** Prof. Emiliano Tramontana  

---

## 📌 Descrizione del Progetto

**Smart University** è una piattaforma web distribuita a microservizi per la gestione delle carriere universitarie, dell'offerta formativa, degli appelli d'esame e della verbalizzazione dei voti.

Il sistema si compone di 4 moduli Spring Boot indipendenti:
* **`auth-service`** (Porta `8081`): Server di autenticazione e gestione credenziali.
* **`student-service`** (Porta `8082`): Gestione anagrafica studenti e libretto universitario.
* **`exam-service`** (Porta `8083`): Gestione insegnamenti, appelli, prenotazioni e verbalizzazioni.
* **`web-portal`** (Porta `8080`): Interfaccia web utente (Spring Boot MVC + Thymeleaf).

---

## 🛠️ Prerequisiti di Ambiente

Prima di avviare il progetto, assicurarsi che sul sistema siano installati:
* **Java JDK 17** (o versione superiore)
* **Apache Maven 3.8+**
* **Docker Desktop / Docker Engine** (attivo in esecuzione)

---

## 🚀 1. Avvio dell'Infrastruttura Docker (PostgreSQL & RabbitMQ)

Aprire il terminale ed eseguire i seguenti comandi per avviare il Database PostgreSQL e il Broker RabbitMQ con le credenziali e i parametri corrispondenti alle configurazioni del codice (`application.properties`):

```bash
# 1. Avvio container PostgreSQL (con creazione automatica dei DB: auth_db, student_db, exam_db)
docker run -d `
  --name postgres-db `
  -p 5432:5432 `
  -e POSTGRES_USER=admin `
  -e POSTGRES_PASSWORD=adminpassword `
  -e POSTGRES_MULTIPLE_DATABASES=auth_db,student_db,exam_db `
  postgres:15

# 2. Avvio container RabbitMQ (Message Broker per la verbalizzazione asincrona)
docker run -d `
  --name rabbitmq `
  -p 5672:5672 `
  -p 15672:15672 `
  rabbitmq:3-management
```

* **Pannello di Gestione RabbitMQ**: [http://localhost:15672](http://localhost:15672) (Utente: `guest` | Password: `guest`)

---

## 💻 2. Avvio Sequenziale dei Microservizi Spring Boot

Aprire **4 terminali distinti** nelle cartelle dei rispettivi moduli ed avviarli nell'ordine indicato con il comando Maven:

```bash
# Terminale 1: Auth Service (Porta 8081)
cd auth-service
mvn spring-boot:run

# Terminale 2: Student Service (Porta 8082)
cd student-service
mvn spring-boot:run

# Terminale 3: Exam Service (Porta 8083)
cd exam-service
mvn spring-boot:run

# Terminale 4: Web Portal (Porta 8080)
cd web-portal
mvn spring-boot:run
```

---

## 👤 3. Registrazione del Primo Utente Amministratore (`cURL`)

Al primo avvio, il database PostgreSQL risulta privo di utenti. Prima di effettuare il login dall'interfaccia web, eseguire il seguente comando da terminale per registrare l'account **ADMIN** iniziale tramite le API REST di `auth-service`:

### PowerShell (Windows):
```powershell
curl.exe -X POST http://localhost:8081/api/auth/register `
  -H "Content-Type: application/json" `
  -d '{\"username\":\"admin\", \"password\":\"adminpassword\", \"role\":\"ADMIN\"}'
```

### Bash / Linux / macOS:
```bash
curl -X POST http://localhost:8081/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"admin", "password":"adminpassword", "role":"ADMIN"}'
```

Una volta registrato l'amministratore, collegarsi all'interfaccia web su **[http://localhost:8080/login](http://localhost:8080/login)**, accedere con le credenziali appena create (`admin` / `adminpassword`) e registrare i docenti e gli studenti dal pannello di amministrazione.

---

## 🔑 Credenziali di Accesso

| Ruolo | Username | Password | Creazione / Registrazione |
| :--- | :--- | :--- | :--- |
| **`ADMIN`** | `admin` | `adminpassword` | Creato via cURL (Sezione 3). |
| **`DOCENTE`** | `docente1` | `docentepassword` | Registrato dal pannello `/admin/home` dall'Amministratore. |
| **`STUDENTE`** | `studente1` | `studentepassword` | Registrato dal pannello `/admin/home` dall'Amministratore. |

---

## 📋 Guida Rapida ai Test delle Funzionalità

### 👨‍💼 1. Pannello Amministratore (`/admin/home`)
* **Registrazione**: Registrare nuovi utenti specificando ruolo (`DOCENTE` o `STUDENTE`) ed anagrafica.
* **Crea Corso**: Attivare un insegnamento indicando codice, nome e username del docente responsabile (il sistema verifica che lo username appartenga ad un docente).

### 👨‍🏫 2. Pannello Docente (`/docente/home`)
* **Crea Appello**: Aprire un nuovo appello per un insegnamento assegnato.
* **Verbalizza**: Registrarne il voto (18-30 e lode) per gli studenti prenotati.

### 👨‍🎓 3. Portale Studente (`/student/home`)
* **Prenotazione**: Selezionare un appello aperto ed effettuare la prenotazione.
* **Annulla Prenotazione**: Rimuovere una prenotazione prima della chiusura dell'appello.
* **Libretto**: Consultare l'elenco degli esami superati.
