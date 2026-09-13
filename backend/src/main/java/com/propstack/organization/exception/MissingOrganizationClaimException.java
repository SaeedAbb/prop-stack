package com.propstack.organization;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class MissingOrganizationClaimException extends RuntimeException {

    public MissingOrganizationClaimException() {
        super("The authenticated user's token has no organization claim.");
    }
}
