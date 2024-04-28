package service;
import java.util.ArrayList;
import java.util.List;

import bean.EmailBean;
import bean.UserBean;
import dto.*;
import jakarta.inject.Inject;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import utilities.EncryptHelper;


@Path("/users")
public class UserService {
    @Context
    private HttpServletRequest request;
    @Inject
    UserBean userBean;
    @Inject
    EmailBean emailBean;
    @Inject
    EncryptHelper encryptHelper;
    private static final Logger logger = LogManager.getLogger(UserService.class);

    @GET
    @Path("/Deleted")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeletedUsers(@HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting deleted users from IP address " + ipadress);
        boolean user = userBean.tokenExists(token);
        if (!user) {
            logger.info("User with token " + token + " is not found.");
            return Response.status(403).entity("User with this token is not found").build();
        } else {
            List<User> users = userBean.getDeletedUsers();
            logger.info("User with token " + token + " has received deleted users.");
            return Response.status(200).entity(users).build();
        }
    }

    @GET
    @Path("/Active")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveUsers(@HeaderParam("token") String token,@Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting active users from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean user = userBean.tokenExists(token);
        if (!user) {
            logger.info("User with token " + token + " is not found.");
            return Response.status(403).entity("User with this token is not found").build();
        } else {
            List<User> users = userBean.getActiveUsers();
            logger.info("User with token " + token + " has received active users.");
            return Response.status(200).entity(users).build();
        }
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(User a, @HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is adding user from IP address " + ipadress);

        boolean unconfirmed = userBean.isUserUnconfirmed(token);
        if (!unconfirmed) {
            logger.info("User with token " + token + " is not authorized to add user.");
            return Response.status(403).entity("Forbidden").build();
        }
        boolean valid = userBean.isUserValid(a);
        if (!valid) {
            logger.info("User with token " + token + " is trying to add user with invalid data.");
            return Response.status(400).entity("All elements are required are required").build();
        }
        boolean user = userBean.userNameExists(a.getUsername());
        if (user) {
            logger.info("User with token " + token + " is trying to add user with existing username.");
            return Response.status(409).entity("User with this username is already exists").build();
        } else {
            if (a.getRole() == null || a.getRole().isEmpty()) {
                a.setRole("developer");
            }

            if (userBean.addUser(a) && userBean.removeUnconfirmedUser(token)) {
                logger.info("User with token " + token + " has added " + a.getUsername() +" as a user.");
                return Response.status(201).entity("A new user is created").build();
            } else {
                logger.info("User with token " + token + " is trying to add user.");
                return Response.status(400).entity("Failed. User not added").build();
            }
        }
    }

    @GET
    @Path("/photo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPhoto(@HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting photo from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean user = userBean.userExists(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!user) {
            logger.info("User with token " + token + " is not found.");
            return Response.status(404).entity("User with this username is not found").build();
        } else if (!authorized) {
            logger.info("User with token " + token + " is not authorized to get photo.");
            return Response.status(403).entity("Forbidden").build();
        }
        User user1 = userBean.getUser(token);
        if (user1.getUserPhoto() == null) {
            logger.info("User with token " + token + " is trying to get photo with no photo.");
            return Response.status(400).entity("User with no photo").build();
        }
        logger.info("User with token " + token + " has received photo.");
        return Response.status(200).entity(user1.getUserPhoto()).build();
    }

    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@HeaderParam("token") String token, @PathParam("username") String username, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting user from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to get user.");
            return Response.status(403).entity("Forbidden").build();
        }
        boolean exists = userBean.findOtherUserByUsername(username);
        if (!exists) {
            logger.info("User with token " + token + " is trying to get user with non-existing username.");
            return Response.status(404).entity("User with this username is not found").build();
        }
        UserDto user = userBean.getUserByUsername(username);
        logger.info("User with token " + token + " has received user " + username + ".");
        return Response.status(200).entity(user).build();
    }

    @PUT
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(@HeaderParam("token") String token, User a, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is updating user from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean user = userBean.userNameExists(a.getUsername());
        boolean valid = userBean.isUserValid(a);
        if (!user) {
            logger.info("User with token " + token + " is trying to update user with non-existing username.");
            return Response.status(404).entity("User with this username is not found").build();
        } else if (!valid) {
            logger.info("User with token " + token + " is trying to update user with invalid data.");
            return Response.status(406).entity("All elements are required").build();
        }
        if (!userBean.getUser(token).getRole().equals("Owner") || a.getUsername().equals(userBean.getUser(token).getUsername()) && (a.getRole() == null)) {
            a.setRole(userBean.getUser(token).getRole());
            a.setPassword(userBean.getUser(token).getPassword());
            boolean updated = userBean.updateUser(token, a);
            if (!updated) {
                logger.info("User with token " + token + " is trying to update user.");
                return Response.status(400).entity("Failed. User not updated").build();
            }
            logger.info("User with token " + token + " has updated user " + a.getUsername() + ".");
            return Response.status(200).entity("User updated").build();

        } else if (userBean.getUser(token).getRole().equals("Owner") && a.getRole() != null) {
            boolean updated = userBean.ownerupdateUser(token, a);

            if (!updated) {
                logger.info("User with token " + token + " is trying to update user.");
                return Response.status(400).entity("Failed. User not updated").build();
            }
            logger.info("User with token " + token + " has updated user " + a.getUsername() + ".");
            return Response.status(200).entity("User updated").build();
        }
        logger.info("User with token " + token + " is not authorized to update user.");
        return Response.status(403).entity("Forbidden").build();
    }

    @PATCH
    @Path("/password")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePassword(@HeaderParam("token") String token, PasswordDto password, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is updating password from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        boolean valid = userBean.isPasswordValid(password);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to update password.");
            return Response.status(403).entity("Forbidden").build();
        } else if (!valid) {
            logger.info("User with token " + token + " is trying to update password with invalid password.");
            return Response.status(406).entity("Password is not valid").build();
        } else {
            boolean updated = userBean.updatePassword(token, password);
            if (!updated) {
                logger.info("User with token " + token + " is trying to update password.");
                return Response.status(400).entity("Failed. Password not updated").build();
            }
            logger.info("User with token " + token + " has updated password.");
            return Response.status(200).entity("Password updated").build();
        }
    }

    @GET
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@HeaderParam("username") String username, @HeaderParam("password") String password, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with username " + username + " is logging in from IP address " + ipadress);
        if (!userBean.userNameExists(username)) {
            logger.info("User with username " + username + " is not found.");
            return Response.status(404).entity("User with this username is not found").build();
        }
        LoggedUser loggedUser = userBean.login(username, password);
        if (loggedUser == null) {
            logger.info("User with username " + username + " is trying to login.");
            return Response.status(403).entity("User is not active").build();
        } else {
            logger.info("User with username " + username + " has logged in.");
            return Response.status(200).entity(loggedUser).build();

        }
    }

    @GET
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is logging out from IP address " + ipadress);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to logout.");
            return Response.status(405).entity("Forbidden").build();
        } else {
            userBean.logout(token);
            logger.info("User with token " + token + " has logged out.");
            return Response.status(200).entity("Logged out").build();
        }
    }

    @DELETE
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@HeaderParam("token") String token, @PathParam("username") String username, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is deleting user from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to delete user.");
            return Response.status(403).entity("Forbidden").build();
        } else {

            if (userBean.deleteUser(token, username)) {
                logger.info("User with token " + token + " has deleted user.");
                return Response.status(200).entity("User deleted").build();
            } else {
                logger.info("User with token " + token + " is trying to delete user.");
                return Response.status(400).entity("User not deleted").build();
            }
        }
    }

    @GET
    @Path("/myUserDto")
    @Produces(MediaType.APPLICATION_JSON)
    public Response myProfile(@HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting user from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to get user.");
            return Response.status(403).entity("Forbidden").build();
        } else {
            User user = userBean.getUser(token);
            UserDto userDto = userBean.convertUsertoUserDto(user);
            logger.info("User " + user.getUsername()+" with token " + token + " has received user.");
            return Response.status(200).entity(userDto).build();
        }
    }

    @PATCH
    @Path("/active/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restoreUser(@HeaderParam("token") String token, @PathParam("username") String username, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is restoring user from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to restore user.");
            return Response.status(405).entity("Forbidden").build();
        } else {
            if (userBean.restoreUser(username)) {
                logger.info("User with token " + token + " has restored user.");
                return Response.status(200).entity("User restored").build();
            } else {
                logger.info("User with token " + token + " is trying to restore user.");
                return Response.status(400).entity("User not restored").build();
            }
        }
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilteredUsers(@HeaderParam("token") String token, @QueryParam("role") String role, @QueryParam("active") Boolean active, @QueryParam("name") String name, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting filtered users from IP address " + ipadress);
        boolean authorized = userBean.isUserAuthorized(token);
        userBean.setLastActivity(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to get filtered users.");
            return Response.status(403).entity("Forbidden").build();
        } else {

            ArrayList<User> users = userBean.getFilteredUsers(role, active, name);
            System.out.println(users.size());
            logger.info("User with token " + token + " has received filtered users.");
            return Response.status(200).entity(users).build();
        }
    }

    @POST
    @Path("/unconfirmedUser")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUnconfirmedUser(User a, @HeaderParam("token") String token, @Context HttpServletRequest request) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to add unconfirmed user.");
            return Response.status(403).entity("Forbidden").build();
        }
        boolean user = userBean.userNameExists(a.getUsername());
        if (user) {
            logger.info("User with token " + token + " is trying to add unconfirmed user with existing username.");
            return Response.status(409).entity("User with this username is already exists").build();
        } else {
            if (a.getRole() == null || a.getRole().isEmpty()) {
                a.setRole("developer");
            }
            boolean added = userBean.addUnconfirmedUser(a);
            if (!added) {
                logger.info("User with token " + token + " is trying to add unconfirmed user.");
                return Response.status(400).entity("Failed. User not added").build();
            }
            UnconfirmedUser unconfirmedUser = userBean.getUnconfirmedUser(a.getUsername());
            emailBean.sendConfirmationEmail(unconfirmedUser, unconfirmedUser.getToken(), unconfirmedUser.getCreationDate());
            logger.info("User with token " + token + " has added unconfirmed user.");
            return Response.status(201).entity("A new user is created").build();
        }
    }

    @GET
    @Path("/unconfirmedUser/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnconfirmedUser(@PathParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting unconfirmed user from IP address " + ipadress);

        UnconfirmedUser unconfirmedUser = userBean.getUnconfirmedUserByToken(token);
        if (unconfirmedUser == null) {
            logger.info("User with token " + token + " is not found.");
            return Response.status(404).entity("User with this token is not found").build();
        }
        logger.info("User with token " + token + " has received unconfirmed user.");
        return Response.status(200).entity(unconfirmedUser).build();
    }

    @GET
    @Path("/Statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics(@HeaderParam("token") String token, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting statistics from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to get statistics.");
            return Response.status(403).entity("Forbidden").build();
        } else {
            UserStatisticsDto userStatistics = userBean.getStatistics();
            logger.info("User with token " + token + " has received statistics.");
            return Response.status(200).entity(userStatistics).build();
        }
    }

    @POST
    @Path("/passwordRecovery")
    @Produces(MediaType.APPLICATION_JSON)
    public Response passwordRecovery(@HeaderParam("email") String email, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with email " + email + " is trying to recover password from IP address " + ipadress);
        User user = userBean.emailExists(email);
        if (user == null) {
            logger.info("User with email " + email + " is not found.");
            return Response.status(404).entity("User with this email is not found").build();
        } else {
            boolean emailsent = emailBean.sendPasswordResetEmail(user);
            if (!emailsent) {
                logger.info("User with email " + email + " is trying to recover password.");
                return Response.status(400).entity("Failed. Email not sent").build();
            }
            logger.info("User with email " + email + " has recovered password.");
            return Response.status(200).entity("Email sent").build();
        }
    }

    @PATCH
    @Path("/passwordReset/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response passwordReset(@PathParam("token") String token, PasswordDto password, @Context HttpServletRequest request) {
        userBean.setLastActivity(token);

        boolean valid = userBean.isResetPasswordValid(password);
        if (!valid) {
            logger.info("User with token " + token + " is trying to reset password with invalid password.");
            return Response.status(406).entity("Password is not valid").build();
        }
        boolean updated = userBean.passwordReset(token, password.getPassword());
        if (!updated) {
            logger.info("User with token " + token + " is trying to reset password.");
            return Response.status(400).entity("Failed. Password not updated").build();
        }
        logger.info("User with token " + token + " has reset password.");
        return Response.status(200).entity("Password updated").build();
    }

    @GET
    @Path("/messages/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessages(@HeaderParam("token") String token, @PathParam("username") String username, @Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting messages from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to get messages.");
            return Response.status(403).entity("Forbidden").build();
        } else {
            List<MessageDto> messages = userBean.getMessages(token, username);
            logger.info("User with token " + token + " has received messages.");
            return Response.status(200).entity(messages).build();
        }
    }
    @GET
    @Path("/notifications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNotifications(@HeaderParam("token") String token,@Context HttpServletRequest request ) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is getting notifications from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            logger.info("User with token " + token + " is not authorized to get notifications.");
            return Response.status(403).entity("Forbidden").build();
        } else {
            List<NotificationDto> notifications = userBean.getNotifications(token);
            logger.info("User with token " + token + " has received notifications.");
            return Response.status(200).entity(notifications).build();
        }
    }
    @PUT
    @Path("/timeout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response timeout(@HeaderParam("token") String token, Threshold timeout,@Context HttpServletRequest request) {
        String ipadress = request.getRemoteAddr();
        logger.info("User with token " + token + " is setting inactivity threshold from IP address " + ipadress);
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        if (!authorized) {

            logger.info("User with token " + token + " is not authorized to set inactivity threshold.");
            return Response.status(403).entity("Forbidden").build();
        } else {

            userBean.timeout(timeout.getThreshold());
            logger.info("User with token " + token + " has set inactivity threshold to " + timeout.getThreshold()/60 + " minutes.");
            return Response.status(200).entity("Inactivity threshold set to " + timeout.getThreshold()/60 + "minutes").build();
        }
    }
}


