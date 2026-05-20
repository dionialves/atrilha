package dev.zayt.atrilha.notifications;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Fake de {@link EmailVerificationSender} usado em testes: registra cada
 * chamada em vez de enviar SMTP real. Permite asserções sobre destinatário,
 * apelido e token sem subir GreenMail.
 */
public class RecordingEmailSender implements EmailVerificationSender {

    private final List<RecordedEmail> records = new CopyOnWriteArrayList<>();

    @Override
    public void sendVerification(String toEmail, String nickname, UUID token) {
        records.add(new RecordedEmail(toEmail, nickname, token));
    }

    public List<RecordedEmail> recorded() {
        return List.copyOf(records);
    }

    public void clear() {
        records.clear();
    }
}
