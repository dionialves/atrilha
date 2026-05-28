package dev.zayt.atrilha.auth.verification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
@Primary
class NoOpPasswordResetSender implements PasswordResetSender {

    private static final Logger log = LoggerFactory.getLogger(NoOpPasswordResetSender.class);

    @Override
    public void sendReset(String toEmail, String nickname, UUID token) {
        log.debug("PasswordResetSender (no-op): email={}, token={}", toEmail, token);
    }

    @Override
    public boolean isEnabled() {
        return false;
    }
}
