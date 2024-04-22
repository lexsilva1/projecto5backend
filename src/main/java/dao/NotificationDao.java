package dao;
import entities.CategoryEntity;
import entities.NotificationEntity;
import entities.TaskEntity;
import entities.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;

import java.util.ArrayList;
import java.util.List;
@Stateless
public class NotificationDao extends AbstractDao<NotificationEntity> {
    @PersistenceContext
    private EntityManager em;

    public NotificationDao() {
        super(NotificationEntity.class);
    }

    private static final long serialVersionUID = 1L;

    public NotificationEntity findNotificationById(int id) {
        try {
            return (NotificationEntity) em.createNamedQuery("Notification.findNotificationById").setParameter("id", id)
                    .getSingleResult();

        } catch (NoResultException e) {
            return null;
        }

    }

    public ArrayList<NotificationEntity> findNotificationByUser(UserEntity userEntity) {
        try {
            ArrayList<NotificationEntity> notificationEntityEntities = (ArrayList<NotificationEntity>) em.createNamedQuery("Notification.findNotificationByUser").setParameter("user", userEntity).getResultList();
            return notificationEntityEntities;
        } catch (Exception e) {
            return null;
        }
    }

    public NotificationEntity createNotification(NotificationEntity notificationEntity) {
        em.persist(notificationEntity);
        return notificationEntity;
    }

    public String findCreatorByName(String name) {
        try {
            return (String) em.createNamedQuery("Category.findCreatorByName").setParameter("name", name)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }

    public List<NotificationEntity> findUnreadNotificationsByUser(UserEntity userEntity, boolean read) {
        try {
            List<NotificationEntity> notificationEntityEntities = (List<NotificationEntity>) em.createNamedQuery("Notification.findUnreadNotificationsByUser").setParameter("user", userEntity).getResultList();
            return notificationEntityEntities;
        } catch (Exception e) {
            return null;
        }
    }
public boolean update(NotificationEntity notificationEntity) {
        try {
            em.merge(notificationEntity);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
    public NotificationEntity findLatestNotificationByUser(UserEntity userEntity) {
        try {
            return (NotificationEntity) em.createNamedQuery("Notification.findLatestNotificationByUser")
                    .setParameter("user", userEntity)
                    .setMaxResults(1)
                    .getSingleResult();
        } catch (NoResultException e) {
            return null;
        }
    }
}

