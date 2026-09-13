```

\# 🎓 Smart University - Sistema a Microservizi Distribuiti



Progetto per l'esame di \*\*Ingegneria dei Sistemi Distribuiti\*\*  

\*Corso di Laurea Magistrale in Informatica - Università degli Studi di Catania\*  

\*\*Studente:\*\* Fabio Calvagno | \*\*Docente:\*\* Prof. Emiliano Tramontana



\---



\## 🏗️ Architettura del Sistema



Il sistema gestisce i servizi universitari applicando un'architettura a \*\*Microservizi Spring Boot\*\* completamente disaccoppiati, con basi di dati PostgreSQL isolate per ciascun servizio (\*\*Database-per-Service\*\*).



```mermaid

graph TD

&#x20;   subgraph Client Layer

&#x20;       Browser\[Browser dell'Utente]

&#x20;   end



&#x20;   subgraph Presentation Layer

&#x20;       Portal\[Web Portal - Port 8080\\nSpring Boot MVC + Thymeleaf]

&#x20;   end



&#x20;   subgraph Security \&amp; Authentication

&#x20;       AuthService\[AuthService - Port 8081\\nSpring Boot + JWT + Scrypt]

&#x20;       AuthDB\[(PostgreSQL: auth\_db)]

&#x20;       AuthService --\&gt; AuthDB

&#x20;   end



&#x20;   subgraph Business Logic Microservices

&#x20;       StudentService\[StudentService - Port 8082\\nAspectJ Cache + Listener]

&#x20;       StudentDB\[(PostgreSQL: student\_db)]

&#x20;       StudentService --\&gt; StudentDB



&#x20;       ExamService\[ExamService - Port 8083\\nGrade Management]

&#x20;       ExamDB\[(PostgreSQL: exam\_db)]

&#x20;       ExamService --\&gt; ExamDB

&#x20;   end



&#x20;   subgraph Asynchronous Messaging

&#x20;       RabbitMQ((RabbitMQ Broker\\nDirect Exchange: exam-exchange))

&#x20;   end



&#x20;   Browser --\&gt;|HTTP Cookies / HTML| Portal

&#x20;   Portal --\&gt;|REST / Scrypt Auth| AuthService

&#x20;   Portal --\&gt;|REST / Bearer JWT| StudentService

&#x20;   Portal --\&gt;|REST / Bearer JWT| ExamService

&#x20;   ExamService --\&gt;|Publish GradeEvent| RabbitMQ

&#x20;   RabbitMQ --\&gt;|Consume GradeEvent| StudentService



```



\---



\## 🛡️ Design Pattern Implementati



| Requisito / Ambito          | Pattern Applicato                  | Dettaglio Implementativo                                                                 |

| --------------------------- | ---------------------------------- | ---------------------------------------------------------------------------------------- |

| \*\*Autenticazione\*\*          | \*\*Authenticator\*\* \&amp; \*\*Scrypt\*\*     | Centralizzazione login e hash lento password in AuthService.                             |

| \*\*Credenziali\*\*             | \*\*Token (JWT)\*\*                    | Firma HMAC-256 contenente i ruoli del soggetto per verifiche stateless.                  |

| \*\*Controllo Accessi\*\*       | \*\*Reference Monitor (PEP/PDP)\*\*    | JwtInterceptor nei microservizi con verifica firme e scadenze.                           |

| \*\*Autorizzazione\*\*          | \*\*RBAC (Role-Based AC)\*\*           | Policy basate sui ruoli (ADMIN, DOCENTE, STUDENTE).                                      |

| \*\*Session State\*\*           | \*\*Ibrido (Server \&amp; Client)\*\*       | HttpSession sul Web Portal; header Authorization: Bearer verso i backend.                |

| \*\*Trasferimento Dati\*\*      | \*\*DTO\*\* \&amp; \*\*Remote Facade\*\*        | GradeDTO e GradeEvent per disaccoppiare la vista dalle entità JPA.                       |

| \*\*Isolamento Dati\*\*         | \*\*Database-per-Service\*\*           | Basi di dati PostgreSQL separate (auth\\\_db, student\\\_db, exam\\\_db).                      |

| \*\*Comunicazione Asincrona\*\* | \*\*Message Broker (AMQP)\*\*          | RabbitMQ per notificare e registrare i voti in modo disaccoppiato.                       |

| \*\*Tolleranza Duplicati\*\*    | \*\*Idempotent Receiver\*\*            | Deduplicazione preventiva su DB (existsByStudentAndNomeInsegnamento).                    |

| \*\*Resilienza Guasti\*\*       | \*\*Circuit Breaker\*\* \&amp; \*\*Fallback\*\* | Resilience4J in StudentWebController con degradazione controllata se un servizio cade.   |

| \*\*Caching Trasversale\*\*     | \*\*AspectJ AOP\*\*                    | StudentGradeCacheAspect con advice @Around ed invalidazione reattiva su evento RabbitMQ. |



\---



\## ⚡ Guida all'Avvio Rapido



\### 1\\. Prerequisiti



\* \*\*Java 21 (JDK)\*\*

\* \*\*PostgreSQL\*\* attivo con i database `auth\_db`, `student\_db`, `exam\_db`

\* \*\*RabbitMQ Broker\*\* attivo sulla porta standard `5672`



\### 2\\. Esecuzione dei Microservizi



Aprire 4 terminali distinti ed avviare ciascun modulo tramite wrapper Maven:



```

\# Terminale 1: AuthService (Porta 8081)

cd auth-service \&amp;\&amp; ./mvnw spring-boot:run



\# Terminale 2: StudentService (Porta 8082)

cd student-service \&amp;\&amp; ./mvnw spring-boot:run



\# Terminale 3: ExamService (Porta 8083)

cd exam-service \&amp;\&amp; ./mvnw spring-boot:run



\# Terminale 4: WebPortal (Porta 8080)

cd web-portal \&amp;\&amp; ./mvnw spring-boot:run



```



Accedere all'applicazione via browser su: \*\*http://localhost:8080/login\*\*

