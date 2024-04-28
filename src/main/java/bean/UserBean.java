package bean;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.apache.logging.log4j.*;
import Websocket.Chat;
import Websocket.Dashboard;
import dao.*;

import dto.*;
import entities.*;
import dao.*;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import jakarta.inject.Inject;
import jakarta.websocket.Session;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
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
    @Inject
    TimeOutDao timeOutDao;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);
    Gson gson = new Gson();
    private static final Logger logger = LogManager.getLogger(UserBean.class);
    public boolean addUser(User a) {
        logger.info("Adding user: " + a.getUsername());
        if (a.getUsername().isBlank() || a.getName().isBlank() || a.getEmail().isBlank() || a.getContactNumber().isBlank() || a.getUserPhoto().isBlank()) {
            logger.error("User data is blank");
            return false;
        } else if (a.getUsername() == null || a.getName() == null || a.getEmail() == null || a.getContactNumber() == null || a.getUserPhoto() == null) {
            logger.error("User data is null");
            return false;
        }
        logger.info("User data is valid");
        a.setPassword(EncryptHelper.encryptPassword(a.getPassword()));
        logger.info("Password encrypted");
        UserEntity userEntity = convertToEntity(a);
        logger.info("User converted to entity");
        userEntity.setConfirmed(LocalDate.now());
        logger.info("User confirmed");
        userDao.persist(userEntity);
        logger.info("User persisted");
        dashboard.send("ping");
        logger.info("Dashboard pinged");
        return true;
    }

    public void startRemovingExpiredUsers() {
        logger.info("Starting to remove expired unconfirmed users");
        final Runnable remover = new Runnable() {
            public void run() {
                removeExpiredUnconfirmedUsers();
            }
        };
        scheduler.scheduleAtFixedRate(remover, 0, 1, TimeUnit.HOURS);
    }
    public void forcedLogout(String token){
        logger.info("Forced logout of user with token: " + token);
        UserEntity user = userDao.findUserByToken(token);
        logger.info("User found");
        user.setToken(null);
        logger.info("Token set to null");
        user.setLastActivity(null);
        logger.info("Last activity set to null");
        userDao.updateUser(user);
        logger.info("User updated");
    }

    public void removeExpiredUnconfirmedUsers() {
        logger.info("Removing expired unconfirmed users");
        List<UnconfirmedUserEntity> unconfirmedUsers = unconfirmedUSerDao.findAll();
        logger.info("Unconfirmed users found");
        for (UnconfirmedUserEntity unconfirmedUser : unconfirmedUsers) {
            logger.info("Checking expiration date of user: " + unconfirmedUser.getUsername());
            if (unconfirmedUser.getExpirationDate().isBefore(LocalDateTime.now())) {
                logger.info("User expired: " + unconfirmedUser.getUsername());
                unconfirmedUSerDao.remove(unconfirmedUser);
                logger.info("User removed");
            }
        }
    }

    public User getUser(String token) {
        logger.info("Getting user with token: " + token);
        UserEntity userEntity = userDao.findUserByToken(token);
        logger.info("User found");
        return convertToDto(userEntity);
    }

    public User findUserByUsername(String username) {
        logger.info("Finding user by username: " + username);
        UserEntity userEntity = userDao.findUserByUsername(username);
        logger.info("User found");
        return convertToDto(userEntity);
    }


    public List<UserEntity> getUsers() {
        logger.info("Getting all users");
        List<UserEntity> users = userDao.findAll();
        logger.info("Users found");
        return users;
    }

    public boolean blockUser(String username) {
        logger.info("Blocking user: " + username);
        UserEntity a = userDao.findUserByUsername(username);
        logger.info("User found");
        if (a != null) {
            logger.info("User is not null");
            a.setActive(false);
            logger.info("User set to inactive");
            userDao.updateUser(a);
            logger.info("User updated");
            dashboard.send("ping");
            logger.info("Dashboard pinged");
            return true;
        }
        return false;
    }

    public boolean removeUser(String username) {
        logger.info("Removing user: " + username);
        UserEntity a = userDao.findUserByUsername(username);
        logger.info("User found");
        if (a != null) {
            logger.info("User is not null");
            userDao.remove(a);
            logger.info("User removed");
            dashboard.send("ping");
            logger.info("Dashboard pinged");
            return true;
        }
        logger.error("User not found");
        return false;
    }

    public boolean removeUnconfirmedUser(String token) {
        logger.info("Removing unconfirmed user with token: " + token);
        UnconfirmedUserEntity a = unconfirmedUSerDao.findUserByToken(token);
        logger.info("User found");
        if (a != null) {
            logger.info("User is not null");
            unconfirmedUSerDao.remove(a);
            logger.info("User removed");
            dashboard.send("ping");
            logger.info("Dashboard pinged");
            return true;
        }
        return false;
    }

    public boolean ownerupdateUser(String token, User user) {
        logger.info("Owner updating user: " + user.getUsername());
        UserEntity a = userDao.findUserByUsername(user.getUsername());
        logger.info("User found");
        UserEntity responsible = userDao.findUserByToken(token);
        logger.info("Responsible user found");
        if (a != null && responsible.getRole().equals("Owner")) {
            a.setName(user.getName());
            a.setEmail(user.getEmail());
            a.setContactNumber(user.getContactNumber());
            a.setUserPhoto(user.getUserPhoto());
            a.setRole(user.getRole());
            logger.info("User updated");
            userDao.updateUser(a);
            logger.info("Updated user persisted");
            return true;
        }
        logger.error("User not found or responsible user is not owner");
        return false;
    }

    public boolean updateUser(String token, User user) {
        logger.info("Updating user: " + user.getUsername());
        UserEntity a = userDao.findUserByUsername(user.getUsername());
        if (a != null) {
            logger.info("User is not null");
            a.setUsername(user.getUsername());
            a.setName(user.getName());
            a.setEmail(user.getEmail());
            a.setPassword(user.getPassword());
            a.setContactNumber(user.getContactNumber());
            a.setUserPhoto(user.getUserPhoto());
            a.setRole(user.getRole());
            a.setActive(user.isActive());
            logger.info("User updated");
            userDao.updateUser(a);
            logger.info("User persisted");
            return true;
        }
        logger.error("User not found");
        return false;
    }

    public boolean updatePassword(String token, PasswordDto password) {
        logger.info("Updating password");
        UserEntity a = userDao.findUserByToken(token);
        logger.info("User found");
        if (a != null) {
            logger.info("User is not null");
            if (a.getPassword().equals(EncryptHelper.encryptPassword(password.getPassword()))) {
                a.setPassword(EncryptHelper.encryptPassword(password.getNewPassword()));
                logger.info("Password updated");
                userDao.updateUser(a);
                logger.info("User persisted");
                return true;
            }
        }
        logger.error("User not found or password is incorrect");
        return false;
    }

    public boolean isPasswordValid(PasswordDto password) {
        logger.info("Checking if password is valid");
        if (password.getPassword().isBlank() || password.getNewPassword().isBlank()) {
            logger.error("Password is blank");
            return false;
        } else if (password.getPassword() == null || password.getNewPassword() == null) {
            logger.error("Password is null");
            return false;
        }
        logger.info("Password is valid");
        return true;
    }
    public boolean isResetPasswordValid(PasswordDto password) {
        logger.info("Checking if reset password is valid");
        if (password.getPassword().isBlank()) {
            logger.error("Password is blank");
            return false;
        } else if (password.getPassword() == null) {
            logger.error("Password is null");
            return false;
        }
        logger.info("Password is valid");
        return true;
    }

    public boolean findOtherUserByUsername(String username) {
        logger.info("Finding user by username: " + username);
        UserEntity a = userDao.findUserByUsername(username);
        return a != null;
    }

    public LoggedUser login(String username, String password) {
        logger.info("Logging in user: " + username);
        UserEntity user = userDao.findUserByUsername(username);
        String password1 = EncryptHelper.encryptPassword(password);
        if (user != null && user.isActive()) {
            logger.info("User found and active");
            String token;
            logger.info("Generating token");
            if (user.getPassword().equals(password1)) {
                do {
                    token = generateToken();
                } while (tokenExists(token));
            } else {
                logger.error("Password is incorrect");
                return null;
            }
            logger.info("Token generated");
            user.setToken(token);
            user.setLastActivity(LocalDateTime.now());
            userDao.updateToken(user);
            logger.info("User updated");
            return convertEntityToLoggedUser(user);
        }
        logger.error("User not found or not active");
        return null;
    }

    public boolean userExists(String token) {
        logger.info("Checking if user exists");
        UserEntity a = userDao.findUserByToken(token);
        if (a != null) {
            logger.info("User exists");
            return true;
        }
        logger.error("User does not exist");
        return false;
    }


    public boolean userNameExists(String username) {
        logger.info("Checking if username exists");
        UserEntity a = userDao.findUserByUsername(username);
        if (a != null) {
            logger.info("Username exists");
            return true;
        }
        logger.error("Username does not exist");
        return false;
    }

    public boolean isUserAuthorized(String token) {
        logger.info("Checking if user is authorized");
        UserEntity a = userDao.findUserByToken(token);
        if (a != null) {
            logger.info("User is authorized");
            return true;
        }
        logger.error("User is not authorized");
        return false;

    }

    public boolean isUserValid(User user) {
        if (user.getUsername().isBlank() || user.getName().isBlank() || user.getEmail().isBlank() || user.getContactNumber().isBlank() || user.getUserPhoto().isBlank()) {
            logger.error("User data is blank");
            return false;
        } else if (user.getUsername() == null || user.getName() == null || user.getEmail() == null || user.getContactNumber() == null || user.getUserPhoto() == null) {
            logger.error("User data is null");
            return false;
        }
        logger.info("User data is valid");
        return true;
    }

    public UserDto getUserByUsername(String username) {
        logger.info("Getting user by username: " + username);
        UserEntity userEntity = userDao.findUserByUsername(username);
        return convertEntitytoUserDto(userEntity);
    }

    public UserEntity getUserEntityByUsername(String username) {
        logger.info("Getting user entity by username: " + username);
        UserEntity userEntity = userDao.findUserByUsername(username);
        return userEntity;
    }

    public UserEntity convertToEntity(User user) {
        logger.info("Converting user to entity");
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
        logger.info("User converted to entity");
        return userEntity;
    }

    public User convertToDto(UserEntity userEntity) {
        logger.info("Converting entity to user");
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
        logger.info("Entity converted to user");
        return user;
    }

    public boolean tokenExists(String token) {
        logger.info("Checking if token exists");
        UserEntity a = userDao.findUserByToken(token);
        return a != null;
    }

    public String generateToken() {
        logger.info("Generating token");
        String token = "";
        for (int i = 0; i < 10; i++) {
            token += (char) (Math.random() * 26 + 'a');
        }
        return token;
    }

    public boolean deleteUser(String token, String username) {
        logger.info("Deleting user: " + username);
        if (username.equals("admin") || username.equals("deleted")) {
            logger.error("Cannot delete admin or deleted user");
            return false;
        }
logger.info("Checking if user is owner");
        UserEntity user = userDao.findUserByUsername(username);
        UserEntity responsible = userDao.findUserByToken(token);
        if (user.isActive() && responsible.getRole().equals("Owner") && !user.getUsername().equals(responsible.getUsername())) {
            logger.info("User is owner");
            user.setActive(false);
            user.setToken(null);
            userDao.updateUser(user);
            logger.info("User updated");
            dashboard.send("ping");
            logger.info("Dashboard pinged");
            return true;
        }
        if (responsible.getRole().equals("Owner") && !user.isActive()) {
            logger.info("User is owner");
            if (doesUserHaveTasks(username)) {
                logger.info("User has tasks");
                List<TaskEntity> tasks = taskBean.getTasksByUser(user);
                UserEntity deletedUser = userDao.findUserByUsername("deleted");
                logger.info("User found");
                for (TaskEntity task : tasks) {
                    logger.info("Checking tasks");
                    task.setUser(deletedUser);
                    logger.info("Task creator uptades to deleted user");
                    taskDao.updateTask(task);
                    logger.info("Task updated");
                    dashboard.send("ping");
                }
            }
            if(doesUserHaveNotifications(username)) {
                logger.info("User has notifications");
                List<NotificationEntity> notifications = notificationDao.findNotificationsByUser(user);
                UserEntity deletedUser = userDao.findUserByUsername("deleted");
                logger.info("User found");
                for (NotificationEntity notification : notifications) {
                    logger.info("Removing Notifications");
                    notificationDao.remove(notification);
                }
            }
            if(doesUserHaveMessages(username)) {
                logger.info("User has messages");
                List<MessageEntity> messages = MessageDao.findMessagesByUser(user);
                for (MessageEntity message : messages) {
                    logger.info("Removing Messages");
                    MessageDao.remove(message);
                }
            }
logger.info("Removing user");
            userDao.remove(user);
            logger.info("User removed");
            dashboard.send("ping");
            return true;
        }
        logger.error("User not found or responsible user is not owner");
        return false;
    }

    public void logout(String token) {
        logger.info("Logging out user with token: " + token);
        UserEntity user = userDao.findUserByToken(token);
        logger.info("User found and setting token to null");
        user.setToken(null);
        user.setLastActivity(null);
        logger.info("User updated");
        userDao.updateToken(user);
    }
    public void timeout(int timeout) {
        logger.info("Setting timeout to: " + timeout);
        TimeOutEntity timeOutEntity = timeOutDao.findTimeOutById(1);
        timeOutEntity.setTimeout(timeout);
        logger.info("Timeout set");
        timeOutDao.updateTimeOut(timeOutEntity);
    }

    public UserDto convertUsertoUserDto(User user) {
        logger.info("Converting user to user dto");
        UserDto userDto = new UserDto();
        userDto.setName(user.getName());
        userDto.setEmail(user.getEmail());
        userDto.setContactNumber(user.getContactNumber());
        userDto.setRole(user.getRole());
        userDto.setUserPhoto(user.getUserPhoto());
        userDto.setUsername(user.getUsername());
        userDto.setActive(user.isActive());
        logger.info("User converted to user dto");
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
        userDto.setActive(user.isActive());
        logger.info("Entity converted to user dto");
        return userDto;
    }

    public boolean isUserOwner(String token) {
        UserEntity a = userDao.findUserByToken(token);
        if (a.getRole().equals("Owner")) {
            logger.info("User is owner");
            return true;
        }
        logger.error("User is not owner");
        return false;
    }

    public boolean restoreUser(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        if (a != null) {
            a.setActive(true);
            userDao.updateUser(a);
            logger.info("User restored");
            return true;
        }
        logger.error("User not found");
        return false;
    }

    public boolean doesUserHaveTasks(String username) {
        logger.info("Checking if user has tasks");
        UserEntity a = userDao.findUserByUsername(username);
        List<TaskEntity> tasks = taskBean.getTasksByUser(a);
        if (tasks.size() > 0) {
            logger.info("User has tasks");
            return true;
        } else {
            logger.error("User does not have tasks");
            return false;
        }
    }
    public boolean doesUserHaveNotifications(String username) {
        logger.info("Checking if user has notifications");
        UserEntity a = userDao.findUserByUsername(username);
        List<NotificationEntity> notifications = notificationDao.findNotificationsByUser(a);
        if (notifications.size() > 0) {
            logger.info("User has notifications");
            return true;
        } else {
            logger.error("User does not have notifications");
            return false;
        }
    }
    public boolean doesUserHaveMessages(String username) {
        logger.info("Checking if user has messages");
        UserEntity a = userDao.findUserByUsername(username);
        List<MessageEntity> messages = MessageDao.findMessagesByUser(a);
        if (messages.size() > 0) {
            logger.info("User has messages");
            return true;
        } else {
            logger.error("User does not have messages");
            return false;
        }
    }

    public void createDefaultUsers() {
        logger.info("Creating default users");
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
            logger.info("Admin user created");
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
            logger.info("Deleted user created");
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
            logger.info("Gabrielle Smith user created");
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
            logger.info("John Doe user created");
        }
    }
    public void setLastActivity(String token) {
        logger.info("Setting last activity");
        UserEntity user = userDao.findUserByToken(token);
        user.setLastActivity(LocalDateTime.now());
        userDao.updateUser(user);
        logger.info("Last activity set");
    }

    public ArrayList<User> getDeletedUsers() {
        logger.info("Getting deleted users");
        List<UserEntity> users = userDao.getDeletedUsers();
        ArrayList<User> usersDto = new ArrayList<>();
        for (UserEntity user : users) {
            usersDto.add(convertToDto(user));
        }
        logger.info("Deleted users found");
        return usersDto;
    }

    public ArrayList<User> getActiveUsers() {
        logger.info("Getting active users");
        List<UserEntity> users = userDao.getActiveUsers();
        ArrayList<User> usersDto = new ArrayList<>();
        for (UserEntity user : users) {
            if (!user.getUsername().equals("admin") && !user.getUsername().equals("deleted")) {
                usersDto.add(convertToDto(user));
            }
        }
        logger.info("Active users found");
        return usersDto;
    }

    public ArrayList<User> getAllUsers() {
        logger.info("Getting all users");
        List<UserEntity> users = userDao.findAllUsers();
        ArrayList<User> usersDto = new ArrayList<>();
        for (UserEntity user : users) {
            if(user.getUsername().equals("admin") ) {
                continue;
            }
            usersDto.add(convertToDto(user));
        }
        logger.info("All users found");
        return usersDto;
    }

    public ArrayList<User> getFilteredUsers(String role, Boolean active, String name) {
        logger.info("Getting filtered users");
        ArrayList<User> usersDto = new ArrayList<>();
        if (active == null && role == null && name != null) {
            List<UserEntity> users = userDao.findUsersByName(name);
            for (UserEntity user : users) {
                usersDto.add(convertToDto(user));
            }
            return usersDto;
        }else if(active == null && role == null && name == null) {
            logger.error("No users found");
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
            logger.info("Active users found");
            return usersDto;

        } else if (!active && role != null && name == null) {
            List<UserEntity> users = userDao.getDeletedUsers();
            for (UserEntity user : users) {
                if (user.getRole().equals(role)) {
                    usersDto.add(convertToDto(user));
                }
                logger.info("Deleted users found");
                return usersDto;
            }

        }
        logger.error("No users found");
        return null;
    }





    public MessageEntity sendMessage(MessageDto message) {
        logger.info("Sending message");
        UserEntity sender = userDao.findUserByUsername(message.getSender());
        UserEntity receiver = userDao.findUserByUsername(message.getReceiver());
        if (sender != null && receiver != null) {
            logger.info("Sender and receiver found");
            MessageEntity messageEntity = new MessageEntity();
            messageEntity.setSender(sender);
            messageEntity.setReceiver(receiver);
            messageEntity.setMessage(message.getMessage());
            messageEntity.setTimestamp(LocalDateTime.now());
            messageEntity.setRead(message.isRead());
            MessageDao.persist(messageEntity);
            logger.info("Message persisted");
            return messageEntity;
        }
        logger.error("Sender or receiver not found");
    return null;
    }
    public List<MessageDto> getMessages(String token, String username) {
        logger.info("Getting messages");
        UserEntity sender = userDao.findUserByToken(token);
        UserEntity receiver = userDao.findUserByUsername(username);
        List<MessageEntity> messages = MessageDao.findMessageByUsers(sender, receiver);
        List<MessageDto> messagesDto = new ArrayList<>();
        for (MessageEntity message : messages) {
            logger.info("Converting messages");
            MessageDto messageDto = new MessageDto();
            messageDto.setSender(message.getSender().getUsername());
            messageDto.setReceiver(message.getReceiver().getUsername());
            messageDto.setMessage(message.getMessage());
            messageDto.setSendDate(message.getTimestamp().toString());
            if(message.isRead() == false && message.getReceiver().getUsername().equals(sender.getUsername())) {
                logger.info("Message is not read");
                message.setRead(true);
                MessageDao.merge(message);

            } else {
                logger.info("Message is read");
                messageDto.setRead(message.isRead());
            }
            messagesDto.add(messageDto);
            if(chat.getSession(receiver.getToken(),sender.getUsername()) != null) {
                logger.info("Chat session found");
                MessageDto updateRead = new MessageDto();
                updateRead.setSender(receiver.getUsername());
                updateRead.setReceiver(sender.getUsername());
                updateRead.setMessage("Set All Read");
                logger.info("Sending message to set all read");
                chat.send(receiver.getToken()+"/"+sender.getUsername(), gson.toJson(messageDto));
            }
        }
        logger.info("Messages found");
        return messagesDto;
    }

    public MessageDto convertMessageEntityToDto(MessageEntity message) {
        logger.info("Converting message entity to dto");
        MessageDto messageDto = new MessageDto();
        messageDto.setSender(message.getSender().getUsername());
        messageDto.setReceiver(message.getReceiver().getUsername());
        messageDto.setMessage(message.getMessage());
        messageDto.setSendDate(message.getTimestamp().toString());
        messageDto.setRead(message.isRead());
        logger.info("Message entity converted to dto");
        return messageDto;
    }


    public LoggedUser convertEntityToLoggedUser(UserEntity userEntity) {
        logger.info("Converting entity to logged user");
        LoggedUser loggedUser = new LoggedUser();
        loggedUser.setUsername(userEntity.getUsername());
        loggedUser.setName(userEntity.getName());
        loggedUser.setEmail(userEntity.getEmail());
        loggedUser.setContactNumber(userEntity.getContactNumber());
        loggedUser.setUserPhoto(userEntity.getUserPhoto());
        loggedUser.setRole(userEntity.getRole());
        loggedUser.setToken(userEntity.getToken());
        logger.info("Entity converted to logged user");
        return loggedUser;
    }

    public boolean addUnconfirmedUser(User user) {
        logger.info("Adding unconfirmed user");
        UnconfirmedUserEntity unconfirmedUserEntity = new UnconfirmedUserEntity();
        if (userDao.findUserByUsername(user.getUsername()) != null || user.getUsername() == null || user.getEmail() == null) {
            logger.error("User already exists or username or email is null");
            return false;
        } else if (unconfirmedUSerDao.findUserByUsername(user.getUsername()) != null) {
            logger.error("User already exists");
            return false;
        } else {
            logger.info("User does not exist");
            unconfirmedUserEntity.setUsername(user.getUsername());
            unconfirmedUserEntity.setEmail(user.getEmail());
            unconfirmedUserEntity.setRole(user.getRole());
            unconfirmedUserEntity.setToken(generateToken());
            unconfirmedUserEntity.setExpirationDate(LocalDateTime.now().plusHours(48));
            unconfirmedUserEntity.setCreationDate(LocalDateTime.now());
            unconfirmedUSerDao.addUnconfirmedUser(unconfirmedUserEntity);
            logger.info("Unconfirmed user added");
            return true;
        }
    }

    public UnconfirmedUser getUnconfirmedUser(String username) {
        logger.info("Getting unconfirmed user");
        UnconfirmedUserEntity unconfirmedUserEntity = unconfirmedUSerDao.findUserByUsername(username);
        UnconfirmedUser user = new UnconfirmedUser();
        user.setUsername(unconfirmedUserEntity.getUsername());
        user.setEmail(unconfirmedUserEntity.getEmail());
        user.setRole(unconfirmedUserEntity.getRole());
        user.setToken(unconfirmedUserEntity.getToken());
        user.setCreationDate(unconfirmedUserEntity.getCreationDate());
        user.setExpirationDate(unconfirmedUserEntity.getExpirationDate());
        logger.info("Unconfirmed user found");
        return user;
    }

    public boolean isUserUnconfirmed(String token) {
        logger.info("Checking if user is unconfirmed");
        UnconfirmedUserEntity a = unconfirmedUSerDao.findUserByToken(token);
        logger.info("User found");
        if (a != null) {
            logger.info("User is unconfirmed");
            return true;

        } else
            logger.error("User is not unconfirmed");
            return false;
    }

    public UnconfirmedUser getUnconfirmedUserByToken(String token) {
        logger.info("Getting unconfirmed user by token");
        UnconfirmedUserEntity unconfirmedUserEntity = unconfirmedUSerDao.findUserByToken(token);
        UnconfirmedUser user = new UnconfirmedUser();
        user.setUsername(unconfirmedUserEntity.getUsername());
        user.setEmail(unconfirmedUserEntity.getEmail());
        user.setRole(unconfirmedUserEntity.getRole());
        user.setToken(unconfirmedUserEntity.getToken());
        user.setCreationDate(unconfirmedUserEntity.getCreationDate());
        user.setExpirationDate(unconfirmedUserEntity.getExpirationDate());
        logger.info("Unconfirmed user found");
        return user;

    }

    public UserStatisticsDto getStatistics() {
        logger.info("Getting statistics");
        UserStatisticsDto statisticsDto = new UserStatisticsDto();
        statisticsDto.setTotalUsers(userDao.findAll().size() + unconfirmedUSerDao.findAll().size());
        statisticsDto.setTotalConfirmedusers(userDao.getActiveUsers().size());
        statisticsDto.setTotalBlockedUsers(userDao.getDeletedUsers().size());
        statisticsDto.setTotalUnconfirmedUsers(unconfirmedUSerDao.findAll().size());
        statisticsDto.setConfirmedUsersByDate(countConfirmedUsersByDate());
        logger.info("Statistics found");
        return statisticsDto;
    }

    public Map<LocalDate, Long> countConfirmedUsersByDate() {
        logger.info("Counting confirmed users by date");
        List<Object[]> results = userDao.countConfirmedUsersByDate();
        logger.info("Confirmed users counted");
        return results.stream()
                .collect(Collectors.toMap(
                        result -> (LocalDate) result[0],
                        result -> (Long) result[1]
                ));
    }

    public User emailExists(String email) {
        logger.info("Checking if email exists");
        UserEntity a = userDao.findUserByEmail(email);
        if (a != null) {
            a.setPasswordResetToken(generateToken());
            logger.info("Email exists");
            return convertToDto(a);
        }
        return null;
    }

    public boolean passwordReset(String token, String password) {
        logger.info("Resetting password");
        UserEntity a = userDao.findUserByPasswordResetToken(token);
        if (a != null) {
            a.setPassword(EncryptHelper.encryptPassword(password));
            a.setPasswordResetToken(null);
            userDao.updateUser(a);
            logger.info("Password reset");
            return true;
        }
        logger.error("Password not reset");
        return false;
    }
    public boolean sendNotification(String username, String message, String instance) {
        logger.info("Sending notification");
        UserEntity user = userDao.findUserByUsername(username);
        if (user != null) {
            NotificationEntity notificationEntity = new NotificationEntity();
            notificationEntity.setUser(user);
            notificationEntity.setMessage(message);
            notificationEntity.setTimestamp(LocalDateTime.now());
            notificationEntity.setRead(false);
            notificationEntity.setInstance(instance);
            notificationDao.createNotification(notificationEntity);
            logger.info("Notification sent");
            return true;
        }
        logger.error("Notification not sent");
        return false;
    }
    public List<NotificationDto> getNotifications(String token) {
        logger.info("Getting notifications");
        UserEntity user = userDao.findUserByToken(token);
        List<NotificationEntity> notifications = notificationDao.findUnreadNotificationsByUser(user, false);
        Map<String, Long> unreadCounts = notificationDao.countUnreadNotificationsByUserAndInstance(user);
        logger.info("Notifications found");
        Map<String, List<NotificationEntity>> groupedNotifications = notifications.stream()
                .collect(Collectors.groupingBy(NotificationEntity::getInstance));
        logger.info("Notifications grouped");
        List<NotificationDto> notificationsDto = new ArrayList<>();
        logger.info("Converting notifications");
        for (Map.Entry<String, List<NotificationEntity>> entry : groupedNotifications.entrySet()) {
            NotificationEntity latestNotification = entry.getValue().stream()
                    .max(Comparator.comparing(NotificationEntity::getTimestamp))
                    .orElseThrow(); // or handle the situation when there's no notification

            NotificationDto notificationDto = new NotificationDto();
            notificationDto.setMessage(latestNotification.getMessage());
            notificationDto.setInstance(latestNotification.getInstance());
            notificationDto.setUsername(latestNotification.getUser().getUsername());
            notificationDto.setCount(unreadCounts.getOrDefault(latestNotification.getInstance(), 0L));
            notificationDto.setTimestamp(latestNotification.getTimestamp());

            notificationsDto.add(notificationDto);
        }
        logger.info("Notifications converted");
        return notificationsDto;
    }
}






