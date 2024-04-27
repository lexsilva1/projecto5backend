package entities;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name="Notifications")
@NamedQuery(name="Notification.findNotificationByUser", query="SELECT a FROM NotificationEntity a WHERE a.user = :user")
@NamedQuery(name="Notification.findUnreadNotificationsByUser", query="SELECT a FROM NotificationEntity a WHERE a.user = :user AND a.read = false")
@NamedQuery(name="Notification.findNotificationById", query="SELECT a FROM NotificationEntity a WHERE a.id = :id")
@NamedQuery(name="Notification.findLatestNotificationByUser", query="SELECT a FROM NotificationEntity a WHERE a.user = :user ORDER BY a.timestamp DESC ")
@NamedQuery(name="Notification.findLatestNotificationByUserAndInstance", query="SELECT a FROM NotificationEntity a WHERE a.user = :user AND a.instance = :instance ORDER BY a.timestamp DESC ")
@NamedQuery(name="Notification.findUnreadNotificationsByUserAndInstance", query="SELECT a FROM NotificationEntity a WHERE a.user = :user AND a.read = false AND a.instance = :instance")
@NamedQuery(name="Notification,countUnreadNotificationsByUserAndInstance", query="SELECT COUNT(a) FROM NotificationEntity a WHERE a.user = :user AND a.read = false AND a.instance = :instance")
public class NotificationEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false, unique = true, updatable = false)
    private int id;
    @JoinColumn (name="user", nullable = false, unique = false)
    @ManyToOne
    private UserEntity user;
    @Column (name="message", nullable = false, unique = false, length = 65535, columnDefinition = "TEXT")
    private String message;
    @Column (name="timestamp", nullable = false, unique = false)
    private LocalDateTime timestamp;
    @Column (name="'read'", nullable = false, unique = false)
    private boolean read;
    @Column (name="instance", nullable = false, unique = false)
    private String instance;
    public int getId() {
        return id;
    }
    public void setId(int id) {
        this.id = id;
    }
    public UserEntity getUser() {
        return user;
    }
    public void setUser(UserEntity user) {
        this.user = user;
    }
    public String getMessage() {
        return message;
    }
    public void setMessage(String message) {
        this.message = message;
    }
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    public void setTimestamp(LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }
    public boolean isRead() {
        return read;
    }
    public void setRead(boolean read) {
        this.read = read;
    }
    public String getInstance() {
        return instance;
    }
    public void setInstance(String instance) {
        this.instance = instance;
    }
}
