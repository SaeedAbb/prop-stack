package com.propstack.organization.organization.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.CONFLICT)
public class SelfRemovalNotAllowedException extends RuntimeException {

    public SelfRemovalNotAllowedException() {
        super("An organization admin cannot remove their own membership.");
    }
}
