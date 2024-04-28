package Websocket;
import bean.UserBean;
import dao.MessageDao;
import dao.NotificationDao;
import dto.MessageDto;
import dto.User;
import entities.MessageEntity;
import entities.NotificationEntity;
import entities.UserEntity;
import jakarta.ejb.EJB;
import org.glassfish.tyrus.client.ClientManager;
import org.glassfish.tyrus.client.ClientProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.websocket.*;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

class ChatTest {

    @Mock
    private MessageDao messageDao;

    @Mock
    private UserBean userBean;

    @Mock
    private NotificationDao notificationDao;

    @Mock
    private Notifier notifier;

    @InjectMocks
    private Chat chat;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    public void testToDoOnOpen() {
        Session session = mock(Session.class);
        String token = "sampleToken";
        String username = "sampleUsername";

        User userMock = mock(User.class);
        when(userMock.getUsername()).thenReturn(username);
        when(userBean.getUser(token)).thenReturn(userMock);

        chat.toDoOnOpen(session, token, username);

        Session retrievedSession = chat.getSession(token, username);
        assertNotNull(retrievedSession, "Session should not be null after opening");
    }
    @Test
    void testToDoOnMessage() {

        Session session = mock(Session.class);
        String message = "{\"receiver\":\"receiverUsername\"}";
        String token = "token";
        String username = "username";

        User sender = new User();
        sender.setUsername("senderUsername");
        when(userBean.getUser(token)).thenReturn(sender);

        UserEntity receiverEntity = new UserEntity();
        receiverEntity.setToken("receiverToken");
        receiverEntity.setUsername("receiverUsername");
        when(userBean.getUserEntityByUsername("receiverUsername")).thenReturn(receiverEntity);

        MessageEntity sentMessageEntity = new MessageEntity();
        when(userBean.sendMessage(any(MessageDto.class))).thenReturn(sentMessageEntity);

        NotificationEntity notificationEntity = new NotificationEntity();
        notificationEntity.setUser(receiverEntity);
        when(notificationDao.findLatestNotificationByUser(receiverEntity)).thenReturn(notificationEntity);

        when(userBean.sendNotification(eq("receiverUsername"), anyString(), eq("senderUsername"))).thenReturn(true);
        when(notifier.getSession(receiverEntity.getToken())).thenReturn(mock(Session.class));

        // When
        chat.toDoOnMessage(session, message, token, username);

        // Then
        verify(userBean, times(1)).setLastActivity(token);
        verify(userBean, times(1)).sendMessage(any(MessageDto.class));
        verify(userBean, times(1)).sendNotification(eq("receiverUsername"), anyString(), eq("senderUsername"));
        verify(notificationDao, times(1)).findLatestNotificationByUser(receiverEntity);
        verify(notifier, times(1)).send(eq("receiverToken"), anyString());
    }
}

