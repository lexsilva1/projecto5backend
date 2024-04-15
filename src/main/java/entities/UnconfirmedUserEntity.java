package entities;

import jakarta.persistence.*;

import java.io.Serializable;
import java.time.LocalDateTime;
@Entity
@Table(name="UnconfirmedUsers")
@NamedQuery(name = "UnconfirmedUser.findUserByToken", query = "SELECT u FROM UnconfirmedUserEntity u WHERE u.token = :token")
@NamedQuery(name = "UnconfirmedUser.findUserByUsername", query = "SELECT u FROM UnconfirmedUserEntity u WHERE u.username = :username")
public class UnconfirmedUserEntity implements Serializable {
    @Id
    @Column(name="username", nullable = false, unique = true, updatable = false)
    String username;
    @Column(name="email", nullable = false, unique = true)
    String email;
    @Column(name="creationDate", nullable = false, unique = false)
    LocalDateTime creationDate;
    @Column(name="token", nullable = false, unique = true)
    String token;
    @Column(name="expirationDate", nullable = false, unique = false)
    LocalDateTime expirationDate;
    @Column(name="role", nullable = false, unique = false)
    String role;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public LocalDateTime getCreationDate() {
        return creationDate;
    }

    public void setCreationDate(LocalDateTime creationDate) {
        this.creationDate = creationDate;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public LocalDateTime getExpirationDate() {
        return expirationDate;
    }

    public void setExpirationDate(LocalDateTime expirationDate) {
        this.expirationDate = expirationDate;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public UnconfirmedUserEntity() {

    }


}
