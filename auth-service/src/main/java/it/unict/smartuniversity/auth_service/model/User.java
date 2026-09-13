package it.unict.smartuniversity.auth_service.model;

import jakarta.persistence.*;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Entity
@Table(name = "users") // Mappa la classe sulla tabella 'users' in PostgreSQL
@Data // Genera getter, setter, toString, equals e hashCode (Lombok)
@NoArgsConstructor // Costruttore vuoto richiesto da JPA
@AllArgsConstructor // Costruttore con tutti i campi
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    // Rappresenta l'AuthenticationInfo (la password hashata con Scrypt) [4, 5]
    @Column(nullable = false, name = "password_hash")
    private String password;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Role role; // Il ruolo RBAC assegnato all'utente [3]
}