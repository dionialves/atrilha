package dev.zayt.atrilha.auth.domain;

public enum PasswordResetResult {
    SUCCESS,
    EXPIRED_OR_INVALID,
    ALREADY_USED;
}
