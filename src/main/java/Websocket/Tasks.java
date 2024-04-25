package Websocket;


import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.gson.Gson;
import dto.TaskSocketDto;
import jakarta.ejb.Singleton;
import jakarta.websocket.Session;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import jakarta.websocket.*;
import service.ObjectMapperContextResolver;

import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

@Singleton
@ServerEndpoint("/websocket/tasks/{token}")
public class Tasks {
    HashMap<String, Session> sessions = new HashMap<String, Session>();

    private ObjectMapperContextResolver contextResolver = new ObjectMapperContextResolver();

    public void send(TaskSocketDto taskSocketDto) {
        Iterator<Map.Entry<String, Session>> iterator = sessions.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Session> entry = iterator.next();
            Session session = entry.getValue();
            try {
                ObjectMapper mapper = contextResolver.getContext(null);
                String msg = mapper.writeValueAsString(taskSocketDto);
                session.getBasicRemote().sendText(msg);
            } catch (IOException e) {
                System.out.println("Error in sending message to session " + session.getId() + ": " + e.getMessage());
                // Handle the error as needed
            }
        }
    }
    @OnOpen
    public void onOpen(Session session, @PathParam("token") String token) {
        System.out.println("A new WebSocket session is opened for client with token: " + token);
        sessions.put(token, session);
    }
    @OnClose
    public void onClose(Session session, CloseReason reason) {
        System.out.println("Websocket session is closed with CloseCode: " + reason.getCloseCode() + ": " + reason.getReasonPhrase());
        sessions.entrySet().removeIf(entry -> entry.getValue().equals(session));
    }
    @OnMessage
    public void onMessage(Session session, String msg) {
        System.out.println("Message received: " + msg);
    }
}
