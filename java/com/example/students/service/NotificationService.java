package com.example.students.service;

import com.example.students.model.Teacher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger log = LoggerFactory.getLogger(NotificationService.class);

    private final JavaMailSender mailSender;
    private final boolean mailEnabled;
    private final String fromAddress;

    public NotificationService(ObjectProvider<JavaMailSender> mailSenderProvider,
                               @Value("${app.mail.enabled:true}") boolean enabled,
                               @Value("${app.mail.from:no-reply@students.local}") String fromAddress) {
        this.mailSender = mailSenderProvider.getIfAvailable();
        this.fromAddress = fromAddress;
        this.mailEnabled = enabled && isConfigured(this.mailSender);
        if (!this.mailEnabled) {
            log.info("Mail notifications are disabled. Configure spring.mail properties and set app.mail.enabled=true to enable teacher emails.");
        }
    }

    private boolean isConfigured(JavaMailSender sender) {
        if (sender == null) {
            return false;
        }
        if (sender instanceof JavaMailSenderImpl impl) {
            String host = impl.getHost();
            return host != null && !host.isBlank();
        }
        return true;
    }

    public void sendTeacherWelcome(Teacher teacher) {
        if (!mailEnabled) {
            return;
        }
        if (teacher == null || teacher.getEmail() == null || teacher.getEmail().isBlank()) {
            return;
        }
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromAddress);
            message.setTo(teacher.getEmail());
            message.setSubject("Welcome to Students App");
            message.setText(buildBody(teacher));
            mailSender.send(message);
        } catch (MailException ex) {
            log.warn("Failed to send teacher welcome email to {}: {}", teacher.getEmail(), ex.getMessage());
        }
    }

    private String buildBody(Teacher teacher) {
        StringBuilder sb = new StringBuilder();
        sb.append("Hello ")
          .append(teacher.getName() != null ? teacher.getName() : "Teacher")
          .append(",\n\n")
          .append("Your teacher profile has been created in the Students App.\n");
        if (teacher.getSubjectCode() != null) {
            sb.append("Subject: ").append(teacher.getSubjectCode()).append("\n");
        }
        if (teacher.getDepartment() != null && !teacher.getDepartment().isBlank()) {
            sb.append("Classes: ").append(teacher.getDepartment()).append("\n");
        }
        sb.append("\nLog in using your school account: \n")
          .append("Username: ").append(teacher.getEmail()).append("\n")
          .append("Password: Use the reset link from the Keycloak portal or ask the admin to set a temporary password.\n\n")
          .append("You can begin entering marks at: /teacher/marks\n\n")
          .append("Thanks,\nStudents App Team");
        return sb.toString();
    }
}
