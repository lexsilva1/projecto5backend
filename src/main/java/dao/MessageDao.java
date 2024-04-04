package dao;
import entities.UserEntity;
import jakarta.ejb.Stateless;
import jakarta.persistence.EntityManager;
import jakarta.persistence.NoResultException;
import jakarta.persistence.PersistenceContext;
import entities.MessageEntity;

import java.util.ArrayList;
import java.util.List;
@Stateless
public class MessageDao extends AbstractDao<MessageEntity> {

    @PersistenceContext
    private EntityManager em;
    public MessageDao() {
        super(MessageEntity.class);
    }
    private static final long serialVersionUID = 1L;

    public ArrayList<MessageEntity> findMessageByUsers(UserEntity sender, UserEntity receiver) {
        try {
            ArrayList<MessageEntity> messageEntities = (ArrayList<MessageEntity>) em.createNamedQuery("Message.findMessageByUsers").setParameter("sender", sender).setParameter("receiver", receiver).getResultList();
            return messageEntities;
        } catch (Exception e) {
            return null;
        }
    }

}
