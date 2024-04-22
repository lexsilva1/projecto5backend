package Websocket;
import dao.NotificationDao;
import dto.NotificationDto;
import entities.NotificationEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import com.google.gson.Gson;
@Singleton
@ServerEndpoint("/websocket/notifier/{token}")
public class Notifier {
    @EJB
    private NotificationDao notificationDao;

    HashMap<String, Session> sessions = new HashMap<String, Session>();
    public void send(String token, String msg){
        Session session = sessions.get(token);
        if (session != null){
            System.out.println("sending.......... "+msg);
            try {
                session.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                System.out.println("Something went wrong!");
            }
        }
    }
    Gson gson = new Gson();
    @OnOpen
    public void toDoOnOpen(Session session, @PathParam("token") String token){
        System.out.println("A new WebSocket session is opened for client with token: "+ token);
        sessions.put(token,session);
    }
    @OnClose
    public void toDoOnClose(Session session, CloseReason reason){
        System.out.println("Websocket session is closed with CloseCode: "+ reason.getCloseCode() + ": "+reason.getReasonPhrase());
        for(String key:sessions.keySet()){
            if(sessions.get(key) == session)
                sessions.remove(key);
        }
    }
    @OnMessage
    public void toDoOnMessage(Session session, String msg){
        NotificationDto notificationDto = gson.fromJson(msg, NotificationDto.class);
        NotificationEntity notificationEntity = notificationDao.findNotificationById(notificationDto.getId());
        notificationEntity.setRead(true);
        System.out.println(notificationEntity.isRead());
        boolean read =notificationDao.update(notificationEntity);
        System.out.println("is read: "+read);
        if(read)
            notificationDto.setRead(read);
        try {
            session.getBasicRemote().sendText(gson.toJson(notificationDto));
        } catch (IOException e) {
            System.out.println("Something went wrong!");
        }


    }
}