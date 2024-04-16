package bean;
import dto.UnconfirmedUser;
import dto.User;
import jakarta.ejb.EJB;
import jakarta.ejb.Stateless;
import jakarta.mail.*;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;


import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Properties;

@Stateless
public class EmailBean {

    @EJB
    private UserBean userBean;

    private final String username = "lexsilva0386@outlook.com";
    private final String password = System.getenv("SMTP_PASSWORD");
    private final String host = "smtp-mail.outlook.com";
    private final int port = 587;

    public EmailBean() {
    }

    public boolean sendEmail(String to, String subject, String body) {
        boolean sent = false;

        Properties props = new Properties();
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");
        props.put("mail.smtp.host", host);
        props.put("mail.smtp.port", port);

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(username, password);
            }
        });

        try {
            System.out.println("Sending email to " + to + "...");
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(username));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(to));
            message.setSubject(subject);
            message.setText(body);
            System.out.println("Sending email...");
            Transport.send(message);
            sent = true;
        } catch (MessagingException e) {
            sent = false;
            e.printStackTrace();
        }

        return sent;
    }

    public boolean sendConfirmationEmail(UnconfirmedUser user, String confirmationToken, LocalDateTime creationDate) {
        boolean sent = false;

        String userEmail = user.getEmail();
        String subject = "Scrum - Account Confirmation";
        String confirmationLink = "http://localhost:5173/Confirmation/" + confirmationToken;
        String body = "You have been invited to register with our Scrum Platform "  + ",\n\n"
                + "Please click on the link below to confirm your account.\n\n"
                + "Confirmation Link: " + confirmationLink;

        if (sendEmail(userEmail, subject, body)) {
            sent = true;
        } else {
            // Verifica se já se passaram mais de 48 horas desde a criação do user
            LocalDateTime now = LocalDateTime.now();
            long hoursSinceCreation = ChronoUnit.HOURS.between(creationDate, now);
            if (hoursSinceCreation > 48) {
                userBean.removeUser(user.getUsername());
            }
        }
        return sent;
    }
   public boolean sendPasswordResetEmail(User user) {
        boolean sent = false;

        String userEmail = user.getEmail();
        String subject = "Scrum - Password Reset";
        String resetLink = "http://localhost:5173/PasswordReset/" + user.getPasswordResetToken();
        String body = "You have requested a password reset for your Scrum Platform account " + user.getName() + ",\n\n"
                + "Please click on the link below to reset your password.\n\n"
                + "Password Reset Link: " + resetLink;

        if (sendEmail(userEmail, subject, body)) {
            sent = true;
        }
        return sent;
    }
}
