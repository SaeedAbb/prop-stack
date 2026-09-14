package com.propstack.organization.keycloak.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/** Thrown when a call to Keycloak's Admin REST API fails unexpectedly. */
@ResponseStatus(HttpStatus.BAD_GATEWAY)
public class KeycloakAdminApiException extends RuntimeException {

    public KeycloakAdminApiException(String message) {
        super(message);
    }

    public KeycloakAdminApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
