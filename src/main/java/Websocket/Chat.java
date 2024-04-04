package Websocket;
import dto.MessageDto;
import dto.User;
import jakarta.ejb.Singleton;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import dao.MessageDao;
import entities.MessageEntity;
import entities.UserEntity;
import jakarta.ejb.EJB;
import bean.UserBean;
import com.google.gson.Gson;

@Singleton
@ServerEndpoint("/websocket/chat/{username}")
public class Chat {
    HashMap<String, Session> sessions = new HashMap<String, Session>();
    @EJB
    private MessageDao messageDao;
    @EJB
    private UserBean userBean;

    public void send(String username, String msg) { // tem que se repetir para o sender também
        Session session = sessions.get(username);
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
    public void toDoOnOpen(Session session, @PathParam("username") String username) {
        System.out.println("A new WebSocket session is opened for client: " + username);
        sessions.put(username, session);
    }

    @OnClose
    public void toDoOnClose(Session session, @PathParam("username") String username, CloseReason reason) {
        System.out.println("Websocket session is closed with CloseCode: " + reason.getCloseCode() + ": " + reason.getReasonPhrase());
        sessions.remove(username);
    }

    @OnMessage
    public void toDoOnMessage(Session session, String msg) {
        Gson gson = new Gson();
        MessageDto message = gson.fromJson(msg, MessageDto.class);
        System.out.println("A new message is received: " + message.getMessage());
        userBean.sendMessage(message);
        String jsonmessage= gson.toJson(message);

        // Send the message to the recipient
        send(message.getReceiver(), jsonmessage);
    }
}
