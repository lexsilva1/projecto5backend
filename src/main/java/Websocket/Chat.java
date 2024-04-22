package Websocket;
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

@Singleton
@ServerEndpoint("/websocket/chat/{token}/{username}")
public class Chat {

    HashMap<String, Session> sessions = new HashMap<String, Session>();
    @EJB
    private MessageDao messageDao;
    @EJB
    private UserBean userBean;
    @EJB
    private NotificationDao notificationDao;
    @Inject
    private Notifier notifier;
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
        System.out.println("Message received: " + message);
        Gson gson = new Gson();
        MessageDto messageDto = gson.fromJson(message, MessageDto.class);
        User sender = userBean.getUser(token);
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
        System.out.println("receiver session "+receiverSession);
        System.out.println("sent message "+ sent);
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
            if (sendNotification) {
                NotificationEntity notificationEntity = notificationDao.findLatestNotificationByUser(receiver);
                NotificationDto notificationDto = new NotificationDto();
                notificationDto.setId(notificationEntity.getId());
                notificationDto.setMessage(notificationEntity.getMessage());
                notificationDto.setInstance(notificationEntity.getInstance());
                notificationDto.setUsername(notificationEntity.getUser().getUsername());
                notificationDto.setRead(notificationEntity.isRead());
                notifier.send(receiver.getToken(), gson.toJson(notificationDto));
            }

        }
    }

    }

