package com.eudext.erp.iam.internal.user;

public class UserAlreadyExistsException extends RuntimeException {

    public UserAlreadyExistsException(String email) {
        super("A user with email '" + email + "' already exists in this tenant");
    }
}
