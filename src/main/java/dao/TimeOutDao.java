package dao;

import entities.TimeOutEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Stateless
public class TimeOutDao extends AbstractDao<TimeOutEntity> {
    @PersistenceContext
    private EntityManager em;
    public TimeOutDao() {
        super(TimeOutEntity.class);
    }
    private static final long serialVersionUID = 1L;

    public TimeOutEntity findTimeOutById(int id) {
        return em.find(TimeOutEntity.class, id);
    }

    public void updateTimeOut(TimeOutEntity timeOutEntity) {
        em.merge(timeOutEntity);
    }
    public void createTimeOut(TimeOutEntity timeOutEntity) {
        em.persist(timeOutEntity);
    }
}
