package com.propstack.organization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class NewMemberNameRequiredException extends RuntimeException {

    public NewMemberNameRequiredException() {
        super("firstName and lastName are required when inviting a brand-new user by email.");
    }
}
