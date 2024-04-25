package bean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import Websocket.Chat;
import Websocket.Dashboard;
import dao.*;

import dto.*;
import entities.MessageEntity;
import entities.TaskEntity;
import entities.UnconfirmedUserEntity;
import entities.UserEntity;
import entities.NotificationEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.websocket.Session;
import utilities.EncryptHelper;
import com.google.gson.Gson;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Singleton
public class UserBean {
    public UserBean() {
    }

    @EJB
    UserDao userDao;
    @EJB
    NotificationDao notificationDao;
    @EJB
    UnconfirmedUSerDao unconfirmedUSerDao;
    @EJB
    TaskDao taskDao;
    @EJB
    TaskBean taskBean;
    @EJB
    MessageDao MessageDao;
    @EJB
    EncryptHelper EncryptHelper;
    @EJB
    Dashboard dashboard;
    @EJB
    Chat chat;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    Gson gson = new Gson();
    public boolean addUser(User a) {
        if (a.getUsername().isBlank() || a.getName().isBlank() || a.getEmail().isBlank() || a.getContactNumber().isBlank() || a.getUserPhoto().isBlank()) {
            return false;
        } else if (a.getUsername() == null || a.getName() == null || a.getEmail() == null || a.getContactNumber() == null || a.getUserPhoto() == null) {
            return false;
        }
        a.setPassword(EncryptHelper.encryptPassword(a.getPassword()));
        UserEntity userEntity = convertToEntity(a);
        userEntity.setConfirmed(LocalDate.now());
        userDao.persist(userEntity);
        dashboard.send("ping");
        return true;
    }

    public void startRemovingExpiredUsers() {
        final Runnable remover = new Runnable() {
            public void run() {
                removeExpiredUnconfirmedUsers();
            }
        };
        scheduler.scheduleAtFixedRate(remover, 0, 1, TimeUnit.HOURS);
    }

    public void removeExpiredUnconfirmedUsers() {
        List<UnconfirmedUserEntity> unconfirmedUsers = unconfirmedUSerDao.findAll();
        for (UnconfirmedUserEntity unconfirmedUser : unconfirmedUsers) {
            if (unconfirmedUser.getExpirationDate().isBefore(LocalDateTime.now())) {
                unconfirmedUSerDao.remove(unconfirmedUser);
            }
        }
    }

    public User getUser(String token) {
        UserEntity userEntity = userDao.findUserByToken(token);
        return convertToDto(userEntity);
    }

    public User findUserByUsername(String username) {
        UserEntity userEntity = userDao.findUserByUsername(username);
        return convertToDto(userEntity);
    }


    public List<UserEntity> getUsers() {
        List<UserEntity> users = userDao.findAll();
        return users;
    }

    public boolean blockUser(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        if (a != null) {
            a.setActive(false);
            userDao.updateUser(a);
            dashboard.send("ping");
            return true;
        }
        return false;
    }

    public boolean removeUser(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        if (a != null) {
            userDao.remove(a);
            dashboard.send("ping");
            return true;
        }
        return false;
    }

    public boolean removeUnconfirmedUser(String token) {
        UnconfirmedUserEntity a = unconfirmedUSerDao.findUserByToken(token);
        if (a != null) {
            unconfirmedUSerDao.remove(a);
            dashboard.send("ping");
            return true;
        }
        return false;
    }

    public boolean ownerupdateUser(String token, User user) {
        UserEntity a = userDao.findUserByUsername(user.getUsername());
        UserEntity responsible = userDao.findUserByToken(token);
        if (a != null && responsible.getRole().equals("Owner")) {
            a.setName(user.getName());
            a.setEmail(user.getEmail());
            a.setContactNumber(user.getContactNumber());
            a.setUserPhoto(user.getUserPhoto());
            a.setRole(user.getRole());
            userDao.updateUser(a);
            return true;
        }
        return false;
    }

    public boolean updateUser(String token, User user) {
        UserEntity a = userDao.findUserByUsername(user.getUsername());
        if (a != null) {
            a.setUsername(user.getUsername());
            a.setName(user.getName());
            a.setEmail(user.getEmail());
            a.setPassword(user.getPassword());
            a.setContactNumber(user.getContactNumber());
            a.setUserPhoto(user.getUserPhoto());
            a.setRole(user.getRole());
            a.setActive(user.isActive());
            userDao.updateUser(a);
            return true;
        }
        return false;
    }

    public boolean updatePassword(String token, PasswordDto password) {
        UserEntity a = userDao.findUserByToken(token);
        if (a != null) {
            if (a.getPassword().equals(EncryptHelper.encryptPassword(password.getPassword()))) {
                a.setPassword(EncryptHelper.encryptPassword(password.getNewPassword()));
                userDao.updateUser(a);
                return true;
            }
        }
        return false;
    }

    public boolean isPasswordValid(PasswordDto password) {
        if (password.getPassword().isBlank() || password.getNewPassword().isBlank()) {
            return false;
        } else if (password.getPassword() == null || password.getNewPassword() == null) {
            return false;
        }
        return true;
    }
    public boolean isResetPasswordValid(PasswordDto password) {
        if (password.getPassword().isBlank()) {
            return false;
        } else if (password.getPassword() == null) {
            return false;
        }
        return true;
    }

    public boolean findOtherUserByUsername(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        return a != null;
    }

    public LoggedUser login(String username, String password) {
        UserEntity user = userDao.findUserByUsername(username);
        String password1 = EncryptHelper.encryptPassword(password);
        if (user != null && user.isActive()) {
            String token;
            if (user.getPassword().equals(password1)) {
                do {
                    token = generateToken();
                } while (tokenExists(token));
            } else {
                return null;
            }
            user.setToken(token);
            userDao.updateToken(user);
            return convertEntityToLoggedUser(user);
        }
        return null;
    }

    public boolean userExists(String token) {
        ;
        UserEntity a = userDao.findUserByToken(token);
        if (a != null) {
            return true;
        }
        return false;
    }


    public boolean userNameExists(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        if (a != null) {
            return true;
        }
        return false;
    }

    public boolean isUserAuthorized(String token) {
        UserEntity a = userDao.findUserByToken(token);
        if (a != null) {
            return true;
        }
        return false;

    }

    public boolean isUserValid(User user) {
        if (user.getUsername().isBlank() || user.getName().isBlank() || user.getEmail().isBlank() || user.getContactNumber().isBlank() || user.getUserPhoto().isBlank()) {
            return false;
        } else if (user.getUsername() == null || user.getName() == null || user.getEmail() == null || user.getContactNumber() == null || user.getUserPhoto() == null) {
            return false;
        }
        return true;
    }

    public UserDto getUserByUsername(String username) {
        UserEntity userEntity = userDao.findUserByUsername(username);
        return convertEntitytoUserDto(userEntity);
    }

    public UserEntity getUserEntityByUsername(String username) {
        UserEntity userEntity = userDao.findUserByUsername(username);
        return userEntity;
    }

    public UserEntity convertToEntity(User user) {
        UserEntity userEntity = new UserEntity();
        userEntity.setUsername(user.getUsername());
        userEntity.setName(user.getName());
        userEntity.setEmail(user.getEmail());
        userEntity.setPassword(user.getPassword());
        userEntity.setContactNumber(user.getContactNumber());
        userEntity.setUserPhoto(user.getUserPhoto());
        userEntity.setToken(user.getToken());
        userEntity.setRole(user.getRole());
        userEntity.setActive(user.isActive());
        return userEntity;
    }

    public User convertToDto(UserEntity userEntity) {
        User user = new User();
        user.setUsername(userEntity.getUsername());
        user.setName(userEntity.getName());
        user.setEmail(userEntity.getEmail());
        user.setPassword(userEntity.getPassword());
        user.setContactNumber(userEntity.getContactNumber());
        user.setUserPhoto(userEntity.getUserPhoto());
        user.setToken(userEntity.getToken());
        user.setRole(userEntity.getRole());
        user.setActive(userEntity.isActive());
        user.setPasswordResetToken(userEntity.getPasswordResetToken());
        return user;
    }

    public boolean tokenExists(String token) {
        UserEntity a = userDao.findUserByToken(token);
        return a != null;
    }

    public String generateToken() {
        String token = "";
        for (int i = 0; i < 10; i++) {
            token += (char) (Math.random() * 26 + 'a');
        }
        return token;
    }

    public boolean deleteUser(String token, String username) {
        if (username.equals("admin") || username.equals("deleted")) {
            return false;
        }

        UserEntity user = userDao.findUserByUsername(username);
        UserEntity responsible = userDao.findUserByToken(token);
        if (user.isActive() && responsible.getRole().equals("Owner") && !user.getUsername().equals(responsible.getUsername())) {
            user.setActive(false);
            user.setToken(null);
            userDao.updateUser(user);
            dashboard.send("ping");
            return true;
        }
        if (responsible.getRole().equals("Owner") && !user.isActive()) {
            if (doesUserHaveTasks(username)) {
                List<TaskEntity> tasks = taskBean.getTasksByUser(user);
                UserEntity deletedUser = userDao.findUserByUsername("deleted");
                for (TaskEntity task : tasks) {
                    task.setUser(deletedUser);
                    taskDao.updateTask(task);
                    dashboard.send("ping");
                }
            }
            if(doesUserHaveNotifications(username)) {
                List<NotificationEntity> notifications = notificationDao.findNotificationsByUser(user);
                UserEntity deletedUser = userDao.findUserByUsername("deleted");
                for (NotificationEntity notification : notifications) {
                    notificationDao.remove(notification);
                }
            }
            if(doesUserHaveMessages(username)) {
                List<MessageEntity> messages = MessageDao.findMessagesByUser(user);
                UserEntity deletedUser = userDao.findUserByUsername("deleted");
                for (MessageEntity message : messages) {
                    MessageDao.remove(message);
                }
            }

            userDao.remove(user);
            dashboard.send("ping");
            return true;
        }
        return false;
    }

    public void logout(String token) {
        UserEntity user = userDao.findUserByToken(token);
        user.setToken(null);
        userDao.updateToken(user);
    }

    public UserDto convertUsertoUserDto(User user) {
        UserDto userDto = new UserDto();
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        userDto.setContactNumber(user.getContactNumber());
        userDto.setRole(user.getRole());
        userDto.setUserPhoto(user.getUserPhoto());
        userDto.setUsername(user.getUsername());
        return userDto;
    }

    public UserDto convertEntitytoUserDto(UserEntity user) {
        UserDto userDto = new UserDto();
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        userDto.setContactNumber(user.getContactNumber());
        userDto.setRole(user.getRole());
        userDto.setUserPhoto(user.getUserPhoto());
        userDto.setUsername(user.getUsername());
        userDto.setRole(user.getRole());
        userDto.setTasks(taskBean.getTasksByUser(user).size());
        userDto.setTodoTasks(taskDao.findTasksByUserAndStatus(user, 10).size());
        userDto.setDoingTasks(taskDao.findTasksByUserAndStatus(user, 20).size());
        userDto.setDoneTasks(taskDao.findTasksByUserAndStatus(user, 30).size());
        return userDto;
    }

    public boolean isUserOwner(String token) {
        UserEntity a = userDao.findUserByToken(token);
        if (a.getRole().equals("Owner")) {
            return true;
        }
        return false;
    }

    public boolean restoreUser(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        if (a != null) {
            a.setActive(true);
            userDao.updateUser(a);
            return true;
        }
        return false;
    }

    public boolean doesUserHaveTasks(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        List<TaskEntity> tasks = taskBean.getTasksByUser(a);
        if (tasks.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
    public boolean doesUserHaveNotifications(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        List<NotificationEntity> notifications = notificationDao.findNotificationsByUser(a);
        if (notifications.size() > 0) {
            return true;
        } else {
            return false;
        }
    }
    public boolean doesUserHaveMessages(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        List<MessageEntity> messages = MessageDao.findMessagesByUser(a);
        if (messages.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    public void createDefaultUsers() {
        if (userDao.findUserByUsername("admin") == null) {
            UserEntity userEntity = new UserEntity();
            userEntity.setUsername("admin");
            userEntity.setName("admin");
            userEntity.setEmail("coiso@cenas.com");
            userEntity.setPassword(EncryptHelper.encryptPassword("admin"));
            userEntity.setContactNumber("123456789");
            userEntity.setUserPhoto("https://cdn-icons-png.freepik.com/512/10015/10015419.png");
            userEntity.setRole("Owner");
            userEntity.setActive(true);
            userEntity.setConfirmed(LocalDate.now());
            userDao.persist(userEntity);
        }
        if (userDao.findUserByUsername("deleted") == null) {

            UserEntity userEntity1 = new UserEntity();
            userEntity1.setUsername("deleted");
            userEntity1.setName("Deleted");
            userEntity1.setEmail("ThrowFeces@ppl.com");
            userEntity1.setPassword(EncryptHelper.encryptPassword("deleted"));
            userEntity1.setContactNumber("123456789");
            userEntity1.setUserPhoto("https://www.pngitem.com/pimgs/m/146-1468479_my-profile-icon-blank-profile-picture-circle-hd.png");
            userEntity1.setRole("developer");
            userEntity1.setActive(true);
            userEntity1.setConfirmed(LocalDate.now());
            userDao.persist(userEntity1);
        }
        if(userDao.findUserByUsername("gabsmith") == null) {
            UserEntity userEntity2 = new UserEntity();
            userEntity2.setUsername("gabsmith");
            userEntity2.setName("Gabrielle Smith");
            userEntity2.setEmail("gabinacio@cenas.com");
            userEntity2.setPassword(EncryptHelper.encryptPassword("password"));
            userEntity2.setContactNumber("912334567");
            userEntity2.setUserPhoto("https://www.shutterstock.com/image-photo/beautiful-female-african-american-business-600nw-1601707636.jpg");
            userEntity2.setRole("developer");
            userEntity2.setActive(true);
            userEntity2.setConfirmed(LocalDate.of(2023, 12, 12));
            userDao.persist(userEntity2);
        }
        if(userDao.findUserByUsername("johndoe") == null) {
            UserEntity userEntity3 = new UserEntity();
            userEntity3.setUsername("johndoe");
            userEntity3.setName("John Doe");
            userEntity3.setEmail("johndoe@coiso.com");
            userEntity3.setPassword(EncryptHelper.encryptPassword("password"));
            userEntity3.setContactNumber("912334567");
            userEntity3.setUserPhoto("https://i.kym-cdn.com/entries/icons/facebook/000/016/546/hidethepainharold.jpg");
            userEntity3.setRole("developer");
            userEntity3.setActive(true);
            userEntity3.setConfirmed(LocalDate.of(2024, 02, 29));
            userDao.persist(userEntity3);
        }
    }

    public ArrayList<User> getDeletedUsers() {
        List<UserEntity> users = userDao.getDeletedUsers();
        ArrayList<User> usersDto = new ArrayList<>();
        for (UserEntity user : users) {
            usersDto.add(convertToDto(user));
        }
        return usersDto;
    }

    public ArrayList<User> getActiveUsers() {
        List<UserEntity> users = userDao.getActiveUsers();
        ArrayList<User> usersDto = new ArrayList<>();
        for (UserEntity user : users) {
            if (!user.getUsername().equals("admin") && !user.getUsername().equals("deleted")) {
                usersDto.add(convertToDto(user));
            }
        }
        return usersDto;
    }

    public ArrayList<User> getAllUsers() {
        List<UserEntity> users = userDao.findAllUsers();
        ArrayList<User> usersDto = new ArrayList<>();
        for (UserEntity user : users) {
            if(user.getUsername().equals("admin") ) {
                continue;
            }
            usersDto.add(convertToDto(user));
        }
        return usersDto;
    }

    public ArrayList<User> getFilteredUsers(String role, Boolean active, String name) {
        ArrayList<User> usersDto = new ArrayList<>();
        if (active == null && role == null && name != null) {
            List<UserEntity> users = userDao.findUsersByName(name);
            for (UserEntity user : users) {
                usersDto.add(convertToDto(user));
            }
            return usersDto;
        }else if(active == null && role == null && name == null) {
       return getAllUsers();
        }

        if (active && role == null && name == null) {
            return getActiveUsers();
        } else if (!active && role == null && name == null) {
            return getDeletedUsers();

        } else if (active && role != null && name == null) {
            List<UserEntity> users = userDao.getUsersByRole(role, active);
            for (UserEntity user : users) {
                usersDto.add(convertToDto(user));
            }
            return usersDto;

        } else if (!active && role != null && name == null) {
            List<UserEntity> users = userDao.getDeletedUsers();
            for (UserEntity user : users) {
                if (user.getRole().equals(role)) {
                    usersDto.add(convertToDto(user));
                }
                return usersDto;
            }

        }
        return null;
    }





    public MessageEntity sendMessage(MessageDto message) {
        UserEntity sender = userDao.findUserByUsername(message.getSender());
        UserEntity receiver = userDao.findUserByUsername(message.getReceiver());
        if (sender != null && receiver != null) {
            MessageEntity messageEntity = new MessageEntity();
            messageEntity.setSender(sender);
            messageEntity.setReceiver(receiver);
            messageEntity.setMessage(message.getMessage());
            messageEntity.setTimestamp(LocalDateTime.now());
            messageEntity.setRead(message.isRead());
            MessageDao.persist(messageEntity);
            return messageEntity;
        }
    return null;
    }
    public List<MessageDto> getMessages(String token, String username) {
        UserEntity sender = userDao.findUserByToken(token);
        UserEntity receiver = userDao.findUserByUsername(username);
        List<MessageEntity> messages = MessageDao.findMessageByUsers(sender, receiver);
        List<MessageDto> messagesDto = new ArrayList<>();
        for (MessageEntity message : messages) {
            MessageDto messageDto = new MessageDto();
            messageDto.setSender(message.getSender().getUsername());
            messageDto.setReceiver(message.getReceiver().getUsername());
            messageDto.setMessage(message.getMessage());
            messageDto.setSendDate(message.getTimestamp().toString());
            if(message.isRead() == false && message.getReceiver().getUsername().equals(sender.getUsername())) {
                message.setRead(true);
                MessageDao.merge(message);

            } else {
                messageDto.setRead(message.isRead());
            }
            messagesDto.add(messageDto);
            if(chat.getSession(receiver.getToken(),sender.getUsername()) != null) {
                MessageDto updateRead = new MessageDto();
                updateRead.setSender(receiver.getUsername());
                updateRead.setReceiver(sender.getUsername());
                updateRead.setMessage("Set All Read");
                chat.send(receiver.getToken()+"/"+sender.getUsername(), gson.toJson(messageDto));
            }
        }
        return messagesDto;
    }

    public MessageDto convertMessageEntityToDto(MessageEntity message) {
        MessageDto messageDto = new MessageDto();
        messageDto.setSender(message.getSender().getUsername());
        messageDto.setReceiver(message.getReceiver().getUsername());
        messageDto.setMessage(message.getMessage());
        messageDto.setSendDate(message.getTimestamp().toString());
        messageDto.setRead(message.isRead());
        return messageDto;
    }


    public LoggedUser convertEntityToLoggedUser(UserEntity userEntity) {
        LoggedUser loggedUser = new LoggedUser();
        loggedUser.setUsername(userEntity.getUsername());
        loggedUser.setName(userEntity.getName());
        loggedUser.setEmail(userEntity.getEmail());
        loggedUser.setContactNumber(userEntity.getContactNumber());
        loggedUser.setUserPhoto(userEntity.getUserPhoto());
        loggedUser.setRole(userEntity.getRole());
        loggedUser.setToken(userEntity.getToken());
        return loggedUser;
    }

    public boolean addUnconfirmedUser(User user) {
        UnconfirmedUserEntity unconfirmedUserEntity = new UnconfirmedUserEntity();
        if (userDao.findUserByUsername(user.getUsername()) != null || user.getUsername() == null || user.getEmail() == null) {
            return false;
        } else if (unconfirmedUSerDao.findUserByUsername(user.getUsername()) != null) {
            return false;
        } else {
            unconfirmedUserEntity.setUsername(user.getUsername());
            unconfirmedUserEntity.setEmail(user.getEmail());
            unconfirmedUserEntity.setRole(user.getRole());
            unconfirmedUserEntity.setToken(generateToken());
            unconfirmedUserEntity.setExpirationDate(LocalDateTime.now().plusHours(48));
            unconfirmedUserEntity.setCreationDate(LocalDateTime.now());
            unconfirmedUSerDao.addUnconfirmedUser(unconfirmedUserEntity);
            return true;
        }
    }

    public UnconfirmedUser getUnconfirmedUser(String username) {
        UnconfirmedUserEntity unconfirmedUserEntity = unconfirmedUSerDao.findUserByUsername(username);
        UnconfirmedUser user = new UnconfirmedUser();
        user.setUsername(unconfirmedUserEntity.getUsername());
        user.setEmail(unconfirmedUserEntity.getEmail());
        user.setRole(unconfirmedUserEntity.getRole());
        user.setToken(unconfirmedUserEntity.getToken());
        user.setCreationDate(unconfirmedUserEntity.getCreationDate());
        user.setExpirationDate(unconfirmedUserEntity.getExpirationDate());
        return user;
    }

    public boolean isUserUnconfirmed(String token) {
        UnconfirmedUserEntity a = unconfirmedUSerDao.findUserByToken(token);
        if (a != null) {
            return true;

        } else
            return false;
    }

    public UnconfirmedUser getUnconfirmedUserByToken(String token) {
        UnconfirmedUserEntity unconfirmedUserEntity = unconfirmedUSerDao.findUserByToken(token);
        UnconfirmedUser user = new UnconfirmedUser();
        user.setUsername(unconfirmedUserEntity.getUsername());
        user.setEmail(unconfirmedUserEntity.getEmail());
        user.setRole(unconfirmedUserEntity.getRole());
        user.setToken(unconfirmedUserEntity.getToken());
        user.setCreationDate(unconfirmedUserEntity.getCreationDate());
        user.setExpirationDate(unconfirmedUserEntity.getExpirationDate());
        return user;

    }

    public UserStatisticsDto getStatistics() {
        UserStatisticsDto statisticsDto = new UserStatisticsDto();
        statisticsDto.setTotalUsers(userDao.findAll().size() + unconfirmedUSerDao.findAll().size());
        statisticsDto.setTotalConfirmedusers(userDao.getActiveUsers().size());
        statisticsDto.setTotalBlockedUsers(userDao.getDeletedUsers().size());
        statisticsDto.setTotalUnconfirmedUsers(unconfirmedUSerDao.findAll().size());
        statisticsDto.setConfirmedUsersByDate(countConfirmedUsersByDate());
        return statisticsDto;
    }

    public Map<LocalDate, Long> countConfirmedUsersByDate() {
        List<Object[]> results = userDao.countConfirmedUsersByDate();
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (LocalDate) result[0],
                        result -> (Long) result[1]
                ));
    }

    public User emailExists(String email) {
        UserEntity a = userDao.findUserByEmail(email);
        if (a != null) {
            a.setPasswordResetToken(generateToken());
            return convertToDto(a);
        }
        return null;
    }

    public boolean passwordReset(String token, String password) {
        UserEntity a = userDao.findUserByPasswordResetToken(token);
        if (a != null) {
            a.setPassword(EncryptHelper.encryptPassword(password));
            a.setPasswordResetToken(null);
            userDao.updateUser(a);
            return true;
        }
        return false;
    }
    public boolean sendNotification(String username, String message, String instance) {
        UserEntity user = userDao.findUserByUsername(username);
        if (user != null) {
            NotificationEntity notificationEntity = new NotificationEntity();
            notificationEntity.setUser(user);
            notificationEntity.setMessage(message);
            notificationEntity.setTimestamp(LocalDateTime.now());
            notificationEntity.setRead(false);
            notificationEntity.setInstance(instance);
            notificationDao.createNotification(notificationEntity);
            return true;
        }
        return false;
    }
    public List<NotificationDto> getNotifications(String token) {
        UserEntity user = userDao.findUserByToken(token);
        List<NotificationEntity> notifications = notificationDao.findUnreadNotificationsByUser(user, false);
        List<NotificationDto> notificationsDto = new ArrayList<>();
        for (NotificationEntity notification : notifications) {
            NotificationDto notificationDto = new NotificationDto();
            notificationDto.setMessage(notification.getMessage());
            notificationDto.setInstance(notification.getInstance());
            notificationDto.setUsername(notification.getUser().getUsername());
            notificationDto.setId(notification.getId());
            notificationsDto.add(notificationDto);
        }
        return notificationsDto;
    }
}






