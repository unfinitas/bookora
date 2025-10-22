package fi.unfinitas.bookora.dto.response;

public record VerifyEmailResponse(
    String message,
    UserPublicInfo user
) {}
