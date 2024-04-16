package bean;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import dao.MessageDao;
import dao.TaskDao;
import dao.UnconfirmedUSerDao;
import dao.UserDao;

import dto.*;
import entities.MessageEntity;
import entities.TaskEntity;
import entities.UnconfirmedUserEntity;
import entities.UserEntity;
import jakarta.ejb.EJB;
import jakarta.ejb.Singleton;
import utilities.EncryptHelper;
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
    UnconfirmedUSerDao unconfirmedUSerDao;
    @EJB
    TaskDao taskDao;
    @EJB
    TaskBean taskBean;
    @EJB
    MessageDao MessageDao;
    @EJB
    EncryptHelper EncryptHelper;
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public boolean addUser(User a) {
        if (a.getUsername().isBlank() || a.getName().isBlank() || a.getEmail().isBlank() || a.getContactNumber().isBlank() || a.getUserPhoto().isBlank()) {
            return false;
        } else if (a.getUsername() == null || a.getName() == null || a.getEmail() == null || a.getContactNumber() == null || a.getUserPhoto() == null) {
            return false;
        }
        a.setPassword(EncryptHelper.encryptPassword(a.getPassword()));
        UserEntity userEntity = convertToEntity(a);
        userEntity.setConfirmed(LocalDateTime.now());
        userDao.persist(userEntity);
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
            return true;
        }
        return false;
    }

    public boolean removeUser(String username) {
        UserEntity a = userDao.findUserByUsername(username);
        if (a != null) {
            userDao.remove(a);
            return true;
        }
        return false;
    }

    public boolean removeUnconfirmedUser(String token) {
        UnconfirmedUserEntity a = unconfirmedUSerDao.findUserByToken(token);
        if (a != null) {
            unconfirmedUSerDao.remove(a);
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
            return true;
        }
        if (responsible.getRole().equals("Owner") && !user.isActive()) {
            if (doesUserHaveTasks(username)) {
                List<TaskEntity> tasks = taskBean.getTasksByUser(user);
                UserEntity deletedUser = userDao.findUserByUsername("deleted");
                for (TaskEntity task : tasks) {
                    task.setUser(deletedUser);
                    taskDao.updateTask(task);
                }
            }
            userDao.remove(user);
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
            userEntity.setConfirmed(LocalDateTime.now());
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
            userEntity1.setConfirmed(LocalDateTime.now());
            userDao.persist(userEntity1);
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
            usersDto.add(convertToDto(user));
        }
        return usersDto;
    }

    public ArrayList<User> getFilteredUsers(String role, Boolean active) {
        ArrayList<User> usersDto = new ArrayList<>();
        if (active == null && role == null) {
            return getAllUsers();
        }
        if (active && role == null) {
            return getActiveUsers();
        } else if (!active && role == null) {
            return getDeletedUsers();

        } else if (active && role != null) {
            List<UserEntity> users = userDao.getUsersByRole(role, active);
            for (UserEntity user : users) {
                usersDto.add(convertToDto(user));
            }
            return usersDto;

        } else if (!active && role != null) {
            List<UserEntity> users = userDao.getDeletedUsers();
            for (UserEntity user : users) {
                if (user.getRole().equals(role)) {
                    usersDto.add(convertToDto(user));
                }
            }
            return usersDto;
        } else if (!active && role == null) {
            List<UserEntity> users = userDao.getDeletedUsers();
            for (UserEntity user : users) {
                usersDto.add(convertToDto(user));
            }
            return usersDto;
        }
        return usersDto;

    }

    public void sendMessage(MessageDto messageDto) {
        UserEntity sender = userDao.findUserByUsername(messageDto.getSender());
        UserEntity receiver = userDao.findUserByUsername(messageDto.getReceiver());
        if (sender != null && receiver != null) {
            MessageEntity message = new MessageEntity();
            message.setSender(sender);
            message.setReceiver(receiver);
            message.setMessage(messageDto.getMessage());
            message.setTimestamp(LocalDateTime.parse(messageDto.getSendDate()));
            message.setRead(false);
            System.out.println("Message sent from " + sender.getUsername() + " to " + receiver.getUsername() + " at " + message.getTimestamp());
            MessageDao.persist(message);

        }

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
        statisticsDto.setTotalUnconfirmedUsers(unconfirmedUSerDao.findAll().size());
        statisticsDto.setConfirmedUsersByDate(countConfirmedUsersByDate());
        return statisticsDto;
    }

    public List<ConfirmedUserDto> countConfirmedUsersByDate() {
        List<Object[]> results = userDao.countConfirmedUsersByDate();
        return results.stream()
                .map(result -> new ConfirmedUserDto((LocalDateTime) result[0], (Long) result[1]))
                .collect(Collectors.toList());
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
}






