package bean;

import Websocket.Notifier;
import bean.UserBean;
import dao.TimeOutDao;
import dao.UserDao;
import dto.LogOutNotification;
import dto.NotificationDto;
import entities.TimeOutEntity;
import entities.UserEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Schedule;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import com.google.gson.Gson;
import service.ObjectMapperContextResolver;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;

@Singleton
public class TimerBean {

    @EJB
    private UserDao userDao;
    @Inject
    Notifier notifier;
    @Inject
    TimeOutDao timeOutDao;
    @EJB
    private UserBean userBean;
    private Gson gson = new Gson();
    @Schedule(hour = "*", minute = "*", second = "*/30", persistent = false)
    public void checkTimeouts() {
        HashMap<String, LocalDateTime> tokensAndTimeouts = userDao.findAllTokensAndTimeouts();
        TimeOutEntity timeOutEntity = timeOutDao.findTimeOutById(1);
        int timeoutLimit = timeOutEntity.getTimeout();

        for (String token : tokensAndTimeouts.keySet()) {
            LocalDateTime lastAction = tokensAndTimeouts.get(token);
            if (lastAction == null) {
                continue; // Skip this iteration if lastAction is null
            }
            LocalDateTime now = LocalDateTime.now();
            if (Duration.between(lastAction, now).getSeconds() > timeoutLimit) {
                LogOutNotification notificationDto = new LogOutNotification();
                userBean.forcedLogout(token);
                notifier.send(token, gson.toJson(notificationDto));
                System.out.println("User with token " + token + " has been logged out due to inactivity.");
            }
        }
    }
    public void createTimeout(int timeout) {
        if(timeOutDao.findTimeOutById(1) == null) {
            TimeOutEntity timeOutEntity = new TimeOutEntity();
            timeOutEntity.setTimeout(timeout);
            timeOutDao.createTimeOut(timeOutEntity);
        }

    }
}
