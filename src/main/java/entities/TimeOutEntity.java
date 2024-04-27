package entities;

import jakarta.persistence.*;
import jdk.jfr.Name;

import java.io.Serializable;

@Entity
@Table(name="TimeOut")
@NamedQuery(name = "TimeOut.findAll", query = "SELECT t FROM TimeOutEntity t")
public class TimeOutEntity implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name="id", nullable = false, unique = true, updatable = false)
    private int id;
    @Column(name="timeout", nullable = false, unique = false)
    private int timeout;


    public int getTimeout() {
        return timeout;
    }
    public void setTimeout(int timeout) {
        this.timeout = timeout;
    }
}
