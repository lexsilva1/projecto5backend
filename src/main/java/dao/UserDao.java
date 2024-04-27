package dao;

import entities.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

@Stateless
public class UserDao extends AbstractDao<UserEntity>{
    @PersistenceContext
    private EntityManager em;
    private static final long serialVersionUID = 1L;
    public UserDao() {
        super(UserEntity.class);
    }
    public UserEntity findUserByToken(String token) {
        try {
            return (UserEntity) em.createNamedQuery("User.findUserByToken").setParameter("token", token)
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }
    public UserEntity findUserByUsername(String username) {
        try {
            return (UserEntity) em.createNamedQuery("User.findUserByUsername").setParameter("username", username)
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }
    public List<UserEntity> getUsersByRole(String role, Boolean active) {
        return em.createNamedQuery("User.findUserByRole").setParameter("role", role).setParameter("active", active).getResultList();
    }


    public List<UserEntity> getDeletedUsers() {
        return em.createNamedQuery("User.findDeletedUsers").getResultList();
    }
    public void updateToken(UserEntity userEntity) {
        em.createNamedQuery("User.updateToken").setParameter("token", userEntity.getToken()).setParameter("username",userEntity.getName()).executeUpdate();
    }
    public void updateUser(UserEntity userEntity) {
        em.merge(userEntity);
    }
    public List<UserEntity> getActiveUsers() {
        return em.createNamedQuery("User.findActiveUsers").getResultList();
    }

    public List<UserEntity> findAllUsers() {
        return em.createNamedQuery("User.findAllUsers").getResultList();
    }
    public List<Object[]> countConfirmedUsersByDate() {
        return em.createNamedQuery("User.countConfirmedUsersByDate").getResultList();
    }
    public UserEntity findUserByEmail(String email) {
        try {
            return (UserEntity) em.createNamedQuery("User.findUserByEmail").setParameter("email", email)
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }
    public UserEntity findUserByPasswordResetToken(String passwordResetToken) {
        try {
            return (UserEntity) em.createNamedQuery("User.FindUserByPasswordResetToken").setParameter("passwordResetToken", passwordResetToken)
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }
    }
    public List<UserEntity> findUsersByName(String name) {
        return em.createNamedQuery("User.findUsersByName").setParameter("name","%"+ name+"%").getResultList();
    }
    public HashMap<String, LocalDateTime> findAllTokensAndTimeouts() {
        List<Object[]> results = em.createNamedQuery("User.findAllTokensAndTimeouts").getResultList();
        HashMap<String, LocalDateTime> tokensAndTimeouts = new HashMap<>();
        for (Object[] result : results) {
            tokensAndTimeouts.put((String) result[0], (LocalDateTime) result[1]);
        }
        return tokensAndTimeouts;
    }
}
