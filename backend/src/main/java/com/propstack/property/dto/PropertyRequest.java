package com.propstack.property.dto;

import com.propstack.property.PropertyStatus;
import com.propstack.property.PropertyType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PropertyRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String address;

    @NotNull
    private PropertyType type;

    @NotNull
    private PropertyStatus status;
}
