package com.propstack.property.property.exception;

import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class PropertyNotFoundException extends RuntimeException {

    public PropertyNotFoundException(UUID id) {
        super("Property not found: " + id);
    }
}
