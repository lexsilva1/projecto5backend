package Websocket;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import dao.NotificationDao;
import dto.MessageDto;
import dto.User;
import dto.UserDto;
import entities.NotificationEntity;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;

import dao.MessageDao;
import entities.MessageEntity;
import entities.UserEntity;
import jakarta.ejb.EJB;
import bean.UserBean;
import com.google.gson.Gson;
import dto.NotificationDto;
import service.ObjectMapperContextResolver;

@Singleton
@ServerEndpoint("/websocket/chat/{token}/{username}")
public class Chat {
    public void setUserBean(UserBean userBean) {
        this.userBean = userBean;
    }
    public void serNotifier(Notifier notifier) {
        this.notifier = notifier;
    }
    HashMap<String, Session> sessions = new HashMap<String, Session>();
    @EJB
    private MessageDao messageDao;
    @EJB
    private UserBean userBean;
    @EJB
    private NotificationDao notificationDao;
    @Inject
    private Notifier notifier;
    private ObjectMapperContextResolver contextResolver = new ObjectMapperContextResolver();
    public Session getSession(String token, String username) {
        String conversationToken = token + "/" + username;
        return sessions.get(conversationToken);
    }


    public void send(String token, String msg) { // tem que se repetir para o sender também
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

    @OnOpen
    public void toDoOnOpen(Session session, @PathParam("token") String token,@PathParam("username") String username) {
        User user = userBean.getUser(token);
        String conversationToken = token+"/"+username;
        System.out.println("A new WebSocket session is opened for client: " + user.getUsername());
        sessions.put(conversationToken, session);
    }

    @OnClose
    public void toDoOnClose(Session session, @PathParam("token") String token, @PathParam("username") String username, CloseReason reason) {
        System.out.println("Websocket session is closed with CloseCode: " + reason.getCloseCode() + ": " + reason.getReasonPhrase());
        String conversationToken = token+"/"+username;
        sessions.remove(conversationToken);
    }

    @OnMessage
    public void toDoOnMessage(Session session, String message, @PathParam("token") String token, @PathParam("username") String username) {
        ObjectMapperContextResolver contextResolver = new ObjectMapperContextResolver();
        Gson gson = new Gson();
        MessageDto messageDto = gson.fromJson(message, MessageDto.class);
        User sender = userBean.getUser(token);
        userBean.setLastActivity(token);
        messageDto.setSender(sender.getUsername());
        UserEntity receiver = (userBean.getUserEntityByUsername(messageDto.getReceiver()));
        String receiverToken = receiver.getToken();
        String conversationToken = receiverToken + "/" + sender.getUsername();
        String myConversationToken = token + "/" + username;
        Session receiverSession = sessions.get(conversationToken);
        if (receiverSession != null) {
            messageDto.setRead(true);
        } else {
            messageDto.setRead(false);

        }
        MessageEntity sent = userBean.sendMessage(messageDto);

        if (sent != null && receiverSession != null) {
            MessageDto sentDto = userBean.convertMessageEntityToDto(sent);
            String sentMessage = gson.toJson(sentDto);
            send(conversationToken, sentMessage);
            send(myConversationToken, sentMessage);
        } else if (sent != null) {
            MessageDto sentDto = userBean.convertMessageEntityToDto(sent);
            send(myConversationToken, gson.toJson(sentDto));
            boolean sendNotification = userBean.sendNotification(receiver.getUsername(), "You have a new message from " + sender.getUsername(), sender.getUsername());
            System.out.println("Notification sent: " + sendNotification);
            if (sendNotification && notifier.getSession(receiverToken) != null) {
                NotificationEntity notificationEntity = notificationDao.findLatestNotificationByUser(receiver);
                NotificationDto notificationDto = new NotificationDto();
                notificationDto.setCount(1);
                notificationDto.setMessage(notificationEntity.getMessage());
                notificationDto.setInstance(notificationEntity.getInstance());
                notificationDto.setUsername(notificationEntity.getUser().getUsername());
                notificationDto.setRead(notificationEntity.isRead());
                notificationDto.setTimestamp(notificationEntity.getTimestamp());
                try {
                    ObjectMapper mapper = contextResolver.getContext(null);
                    String notificationJson = mapper.writeValueAsString(notificationDto);
                    notifier.send(receiverToken, notificationJson);
                } catch (JsonProcessingException e) {
                    System.out.println("Error in converting NotificationDto to JSON: " + e.getMessage());
                    // Handle the error as needed
                }

            }

        }
    }

    }

