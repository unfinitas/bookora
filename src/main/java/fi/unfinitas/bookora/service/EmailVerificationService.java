package fi.unfinitas.bookora.service;

import fi.unfinitas.bookora.domain.model.EmailVerificationToken;
import fi.unfinitas.bookora.domain.model.User;

import java.util.UUID;

public interface EmailVerificationService {

    EmailVerificationToken generateVerificationToken(User user);

    void verifyEmail(UUID token);

    void resendVerificationEmail(String email);
}
