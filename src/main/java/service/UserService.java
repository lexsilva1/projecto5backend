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


    @GET
    @Path("/Deleted")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getDeletedUsers(@HeaderParam("token") String token) {
        boolean user = userBean.tokenExists(token);
        if (!user) {
            return Response.status(403).entity("User with this token is not found").build();
        } else {
            List<User> users = userBean.getDeletedUsers();
            return Response.status(200).entity(users).build();
        }
    }

    @GET
    @Path("/Active")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getActiveUsers(@HeaderParam("token") String token) {
        userBean.setLastActivity(token);
        boolean user = userBean.tokenExists(token);
        if (!user) {
            return Response.status(403).entity("User with this token is not found").build();
        } else {
            List<User> users = userBean.getActiveUsers();
            return Response.status(200).entity(users).build();
        }
    }

    @POST
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    public Response addUser(User a, @HeaderParam("token") String token) {
        userBean.setLastActivity(token);
        boolean unconfirmed = userBean.isUserUnconfirmed(token);
        if (!unconfirmed) {
            return Response.status(403).entity("Forbidden").build();
        }
        boolean valid = userBean.isUserValid(a);
        if (!valid) {
            return Response.status(400).entity("All elements are required are required").build();
        }
        boolean user = userBean.userNameExists(a.getUsername());
        if (user) {

            return Response.status(409).entity("User with this username is already exists").build();
        } else {
            if (a.getRole() == null || a.getRole().isEmpty()) {
                a.setRole("developer");
            }

            if (userBean.addUser(a) && userBean.removeUnconfirmedUser(token)) {
                return Response.status(201).entity("A new user is created").build();
            } else {
                return Response.status(400).entity("Failed. User not added").build();
            }
        }
    }

    @GET
    @Path("/photo")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getPhoto(@HeaderParam("token") String token) {
        userBean.setLastActivity(token);
        boolean user = userBean.userExists(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!user) {
            return Response.status(404).entity("User with this username is not found").build();
        } else if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        }
        User user1 = userBean.getUser(token);
        if (user1.getUserPhoto() == null) {
            return Response.status(400).entity("User with no photo").build();
        }
        return Response.status(200).entity(user1.getUserPhoto()).build();
    }

    @GET
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUser(@HeaderParam("token") String token, @PathParam("username") String username) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        }
        boolean exists = userBean.findOtherUserByUsername(username);
        if (!exists) {
            return Response.status(404).entity("User with this username is not found").build();
        }
        UserDto user = userBean.getUserByUsername(username);
        return Response.status(200).entity(user).build();
    }

    @PUT
    @Path("")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    public Response updateUser(@HeaderParam("token") String token, User a) {
        userBean.setLastActivity(token);
        boolean user = userBean.userNameExists(a.getUsername());
        boolean valid = userBean.isUserValid(a);
        if (!user) {
            return Response.status(404).entity("User with this username is not found").build();
        } else if (!valid) {
            return Response.status(406).entity("All elements are required").build();
        }
        if (!userBean.getUser(token).getRole().equals("Owner") || a.getUsername().equals(userBean.getUser(token).getUsername()) && (a.getRole() == null)) {
            a.setRole(userBean.getUser(token).getRole());
            a.setPassword(userBean.getUser(token).getPassword());
            boolean updated = userBean.updateUser(token, a);
            if (!updated) {
                return Response.status(400).entity("Failed. User not updated").build();
            }
            return Response.status(200).entity("User updated").build();

        } else if (userBean.getUser(token).getRole().equals("Owner") && a.getRole() != null) {
            boolean updated = userBean.ownerupdateUser(token, a);

            if (!updated) {
                return Response.status(400).entity("Failed. User not updated").build();
            }
            return Response.status(200).entity("User updated").build();
        }
        return Response.status(403).entity("Forbidden").build();
    }

    @PATCH
    @Path("/password")
    @Produces(MediaType.APPLICATION_JSON)
    public Response updatePassword(@HeaderParam("token") String token, PasswordDto password) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        boolean valid = userBean.isPasswordValid(password);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        } else if (!valid) {
            return Response.status(406).entity("Password is not valid").build();
        } else {
            boolean updated = userBean.updatePassword(token, password);
            if (!updated) {
                return Response.status(400).entity("Failed. Password not updated").build();
            }
            return Response.status(200).entity("Password updated").build();
        }
    }

    @GET
    @Path("/login")
    @Produces(MediaType.APPLICATION_JSON)
    public Response login(@HeaderParam("username") String username, @HeaderParam("password") String password) {
        if (!userBean.userNameExists(username)) {
            return Response.status(404).entity("User with this username is not found").build();
        }
        LoggedUser loggedUser = userBean.login(username, password);
        if (loggedUser == null) {
            return Response.status(403).entity("User is not active").build();
        } else {

            return Response.status(200).entity(loggedUser).build();

        }
    }

    @GET
    @Path("/logout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response logout(@HeaderParam("token") String token) {
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            return Response.status(405).entity("Forbidden").build();
        } else {
            userBean.logout(token);
            return Response.status(200).entity("Logged out").build();
        }
    }

    @DELETE
    @Path("/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response deleteUser(@HeaderParam("token") String token, @PathParam("username") String username) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        } else {

            if (userBean.deleteUser(token, username)) {
                return Response.status(200).entity("User deleted").build();
            } else {
                return Response.status(400).entity("User not deleted").build();
            }
        }
    }

    @GET
    @Path("/myUserDto")
    @Produces(MediaType.APPLICATION_JSON)
    public Response myProfile(@HeaderParam("token") String token) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        } else {
            User user = userBean.getUser(token);
            UserDto userDto = userBean.convertUsertoUserDto(user);
            return Response.status(200).entity(userDto).build();
        }
    }

    @PATCH
    @Path("/active/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response restoreUser(@HeaderParam("token") String token, @PathParam("username") String username) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        if (!authorized) {
            return Response.status(405).entity("Forbidden").build();
        } else {
            if (userBean.restoreUser(username)) {
                return Response.status(200).entity("User restored").build();
            } else {
                return Response.status(400).entity("User not restored").build();
            }
        }
    }

    @GET
    @Path("")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getFilteredUsers(@HeaderParam("token") String token, @QueryParam("role") String role, @QueryParam("active") Boolean active, @QueryParam("name") String name) {
        boolean authorized = userBean.isUserAuthorized(token);
        userBean.setLastActivity(token);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        } else {

            ArrayList<User> users = userBean.getFilteredUsers(role, active, name);
            System.out.println(users.size());
            return Response.status(200).entity(users).build();
        }
    }

    @POST
    @Path("/unconfirmedUser")
    @Produces(MediaType.APPLICATION_JSON)
    public Response addUnconfirmedUser(User a, @HeaderParam("token") String token) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        }
        boolean user = userBean.userNameExists(a.getUsername());
        if (user) {
            return Response.status(409).entity("User with this username is already exists").build();
        } else {
            if (a.getRole() == null || a.getRole().isEmpty()) {
                a.setRole("developer");
            }
            boolean added = userBean.addUnconfirmedUser(a);
            if (!added) {
                return Response.status(400).entity("Failed. User not added").build();
            }
            UnconfirmedUser unconfirmedUser = userBean.getUnconfirmedUser(a.getUsername());
            emailBean.sendConfirmationEmail(unconfirmedUser, unconfirmedUser.getToken(), unconfirmedUser.getCreationDate());
            return Response.status(201).entity("A new user is created").build();
        }
    }

    @GET
    @Path("/unconfirmedUser/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getUnconfirmedUser(@PathParam("token") String token) {
        userBean.setLastActivity(token);
        UnconfirmedUser unconfirmedUser = userBean.getUnconfirmedUserByToken(token);
        if (unconfirmedUser == null) {
            return Response.status(404).entity("User with this token is not found").build();
        }
        return Response.status(200).entity(unconfirmedUser).build();
    }

    @GET
    @Path("/Statistics")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getStatistics(@HeaderParam("token") String token) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        } else {
            UserStatisticsDto userStatistics = userBean.getStatistics();
            return Response.status(200).entity(userStatistics).build();
        }
    }

    @POST
    @Path("/passwordRecovery")
    @Produces(MediaType.APPLICATION_JSON)
    public Response passwordRecovery(@HeaderParam("email") String email) {

        User user = userBean.emailExists(email);
        if (user == null) {
            return Response.status(404).entity("User with this email is not found").build();
        } else {
            boolean emailsent = emailBean.sendPasswordResetEmail(user);
            if (!emailsent) {
                return Response.status(400).entity("Failed. Email not sent").build();
            }
            return Response.status(200).entity("Email sent").build();
        }
    }

    @PATCH
    @Path("/passwordReset/{token}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response passwordReset(@PathParam("token") String token, PasswordDto password) {
        userBean.setLastActivity(token);

        boolean valid = userBean.isResetPasswordValid(password);
        if (!valid) {
            return Response.status(406).entity("Password is not valid").build();
        }
        boolean updated = userBean.passwordReset(token, password.getPassword());
        if (!updated) {
            return Response.status(400).entity("Failed. Password not updated").build();
        }
        return Response.status(200).entity("Password updated").build();
    }

    @GET
    @Path("/messages/{username}")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getMessages(@HeaderParam("token") String token, @PathParam("username") String username) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        } else {
            List<MessageDto> messages = userBean.getMessages(token, username);
            return Response.status(200).entity(messages).build();
        }
    }
    @GET
    @Path("/notifications")
    @Produces(MediaType.APPLICATION_JSON)
    public Response getNotifications(@HeaderParam("token") String token) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserAuthorized(token);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        } else {
            List<NotificationDto> notifications = userBean.getNotifications(token);
            return Response.status(200).entity(notifications).build();
        }
    }
    @PUT
    @Path("/timeout")
    @Produces(MediaType.APPLICATION_JSON)
    public Response timeout(@HeaderParam("token") String token, Threshold timeout) {
        userBean.setLastActivity(token);
        boolean authorized = userBean.isUserOwner(token);
        if (!authorized) {
            return Response.status(403).entity("Forbidden").build();
        } else {
            System.out.println("timeout " +timeout);
            userBean.timeout(timeout.getThreshold());
            return Response.status(200).entity("Inactivity threshold set to " + timeout.getThreshold()/60 + "minutes").build();
        }
    }
}


