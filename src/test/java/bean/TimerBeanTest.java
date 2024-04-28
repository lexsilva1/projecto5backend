package bean;

import Websocket.Notifier;
import bean.TimerBean;
import bean.UserBean;
import dao.TimeOutDao;
import dao.UserDao;
import entities.TimeOutEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Field;
import java.time.LocalDateTime;
import java.util.HashMap;

import static org.mockito.Mockito.*;

class TimerBeanTest {
    @Mock
    private UserDao userDao;
    @Mock
    private Notifier notifier;
    @Mock
    private TimeOutDao timeOutDao;
    @Mock
    private UserBean userBean;

    private TimerBean timerBean;

    @BeforeEach
    void setUp() throws NoSuchFieldException, IllegalAccessException {
        MockitoAnnotations.openMocks(this);

        timerBean = new TimerBean();

        Field userDaoField = TimerBean.class.getDeclaredField("userDao");
        userDaoField.setAccessible(true);
        userDaoField.set(timerBean, userDao);

        Field notifierField = TimerBean.class.getDeclaredField("notifier");
        notifierField.setAccessible(true);
        notifierField.set(timerBean, notifier);

        Field timeOutDaoField = TimerBean.class.getDeclaredField("timeOutDao");
        timeOutDaoField.setAccessible(true);
        timeOutDaoField.set(timerBean, timeOutDao);

        Field userBeanField = TimerBean.class.getDeclaredField("userBean");
        userBeanField.setAccessible(true);
        userBeanField.set(timerBean, userBean);
    }

    @Test
    void testCheckTimeouts() {
        // Given
        HashMap<String, LocalDateTime> tokensAndTimeouts = new HashMap<>();
        tokensAndTimeouts.put("token", LocalDateTime.now().minusMinutes(10)); // Simulate a user who has been inactive for 10 minutes

        when(userDao.findAllTokensAndTimeouts()).thenReturn(tokensAndTimeouts);

        TimeOutEntity timeOutEntity = new TimeOutEntity();
        timeOutEntity.setTimeout(5); // Set the timeout limit to 5 minutes

        when(timeOutDao.findTimeOutById(1)).thenReturn(timeOutEntity);

        // When
        timerBean.checkTimeouts();

        // Then
        verify(userBean, times(1)).forcedLogout("token");
        verify(notifier, times(1)).send(eq("token"), anyString());
    }

    @Test
    void testCreateTimeout() {
        // Given
        when(timeOutDao.findTimeOutById(1)).thenReturn(null);

        // When
        timerBean.createTimeout(5);

        // Then
        verify(timeOutDao, times(1)).createTimeOut(any(TimeOutEntity.class));
    }
}
