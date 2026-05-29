package dev.zayt.atrilha.auth.verification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Profile("test")
class NoOpPasswordResetSender implements PasswordResetSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpPasswordResetSender.class);

    @Override
    public void sendReset(String toEmail, String nickname, UUID token) {
        log.debug("PasswordResetSender (no-op): email={}, token sent", toEmail);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
