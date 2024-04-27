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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

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

    public ArrayList<NotificationEntity> findNotificationsByUser(UserEntity userEntity) {
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
    public Map<String, Long> countUnreadNotificationsByUserAndInstance(UserEntity userEntity) {
        try {
            List<Object[]> results = em.createQuery("SELECT n.instance, COUNT(n) FROM NotificationEntity n WHERE n.user = :user AND n.read = false GROUP BY n.instance")
                    .setParameter("user", userEntity)
                    .getResultList();
            return results.stream()
                    .collect(Collectors.toMap(
                            result -> (String) result[0],
                            result -> (Long) result[1]
                    ));
        } catch (Exception e) {
            return Collections.emptyMap();
        }
    }
    public List<NotificationEntity> findUnreadNotificationsByUserAndInstance(UserEntity userEntity, String instance) {
        try {
            return em.createNamedQuery("Notification.findUnreadNotificationsByUserAndInstance", NotificationEntity.class)
                    .setParameter("user", userEntity)
                    .setParameter("instance", instance)
                    .getResultList();
        } catch (NoResultException e) {
            return null;
        }
    }

}

