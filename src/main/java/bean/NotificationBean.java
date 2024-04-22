package bean;

import Websocket.Chat;
import Websocket.Notifier;
import dao.NotificationDao;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import java.io.IOException;
import java.util.HashMap;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import dao.TaskDao;
import dao.UserDao;
import bean.UserBean;
import dto.Task;
import entities.CategoryEntity;
import entities.TaskEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Startup;
import jakarta.ejb.Stateless;

@Singleton
public class NotificationBean {
    public NotificationBean() {
    }
    @EJB
    private TaskDao taskDao;
    @EJB
    private UserDao userDao;
    @EJB
    private UserBean userBean;
    @EJB
    private NotificationDao notificationDao;
    @Inject
    Notifier notifier;
    @Inject
    Chat chat;

}
