package pipelines.infrastructure;

import org.springframework.stereotype.Component;

@Component
public class ConsoleLoggingEmailService implements EmailService {
    @Override
    public void sendEmail(String to, String subject, String body) {
        System.out.printf("Sending email to: '%s' with subject '%s' and body '%s'  %n", to, subject, body);
    }
}
