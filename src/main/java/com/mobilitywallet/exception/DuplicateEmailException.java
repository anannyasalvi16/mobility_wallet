package com.mobilitywallet.exception;

/**
 * Thrown when an account registration attempts to use an email address that is already registered.
 */
public class DuplicateEmailException extends BusinessException {

    public DuplicateEmailException(String message) {
        super(message);
    }
}
