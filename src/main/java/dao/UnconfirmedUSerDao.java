package dao;

import entities.UnconfirmedUserEntity;
import entities.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.util.List;

@Stateless

public class UnconfirmedUSerDao extends AbstractDao<UnconfirmedUserEntity> {
    @PersistenceContext
    private EntityManager em;
    private static final long serialVersionUID = 1L;

    public UnconfirmedUSerDao() {
        super(UnconfirmedUserEntity.class);
    }

    public UnconfirmedUserEntity findUserByToken(String token) {
        try {
            return (UnconfirmedUserEntity) em.createNamedQuery("UnconfirmedUser.findUserByToken").setParameter("token", token)
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }

    public UnconfirmedUserEntity findUserByUsername(String username) {
        try {
            return (UnconfirmedUserEntity) em.createNamedQuery("UnconfirmedUser.findUserByUsername").setParameter("username", username)
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }
    public void addUnconfirmedUser(UnconfirmedUserEntity unconfirmedUserEntity) {
        em.persist(unconfirmedUserEntity);
    }

}
