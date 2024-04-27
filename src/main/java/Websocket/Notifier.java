package Websocket;

import bean.UserBean;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.NotificationDao;
import dto.NotificationDto;
import entities.NotificationEntity;
import entities.UserEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import service.ObjectMapperContextResolver;

    @Singleton
    @ServerEndpoint("/websocket/notifier/{token}")
    public class Notifier {
        @EJB
        private NotificationDao notificationDao;
        @EJB
        private UserBean userBean;

        HashMap<String, Session> sessions = new HashMap<String, Session>();
        private ObjectMapperContextResolver contextResolver = new ObjectMapperContextResolver();

        public void send(String token, String msg) {
            Session session = sessions.get(token);
            if (session != null) {
                System.out.println("sending.......... " + msg);
                try {
                    session.getBasicRemote().sendText(msg);
                } catch (IOException e) {
                    System.out.println("Something went wrong!");
                }
            }
        }

        public Session getSession(String token) {
            return sessions.get(token);
        }

        @OnOpen
        public void toDoOnOpen(Session session, @PathParam("token") String token) {
            System.out.println("A new WebSocket session is opened for client with token: " + token);
            sessions.put(token, session);
        }

        @OnClose
        public void toDoOnClose(Session session, CloseReason reason) {
            System.out.println("Websocket session is closed with CloseCode: " + reason.getCloseCode() + ": " + reason.getReasonPhrase());
            for (String key : sessions.keySet()) {
                if (sessions.get(key) == session)
                    sessions.remove(key);
            }
        }

        @OnMessage
        public void toDoOnMessage(Session session, String msg) {
            ObjectMapper mapper = contextResolver.getContext(null);
            NotificationDto notificationDto = null;
            try {
                notificationDto = mapper.readValue(msg, NotificationDto.class);
            } catch (JsonProcessingException e) {
                System.out.println("Error in processing JSON: " + e.getMessage());
            }

            if (notificationDto != null) {
                System.out.println(notificationDto.getUsername() + " sent a message: " + notificationDto.getMessage());
                UserEntity userEntity = userBean.convertToEntity(userBean.findUserByUsername(notificationDto.getUsername()));
                if (!notificationDto.isRead()) {
                    send(userEntity.getToken(), msg);
                } else {
                    List<NotificationEntity> notificationsToBeRead = notificationDao.findUnreadNotificationsByUserAndInstance(userEntity, notificationDto.getInstance());
                    for (NotificationEntity notificationEntity : notificationsToBeRead) {
                        notificationEntity.setRead(true);
                        notificationDao.update(notificationEntity);
                    }
                }
            }
        }
    }