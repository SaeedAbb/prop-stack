package com.propstack.property.web;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.propstack.property.dto.AddressRequest;
import com.propstack.property.dto.AddressResponse;
import com.propstack.property.dto.PropertyRequest;
import com.propstack.property.dto.PropertyResponse;
import com.propstack.property.exception.PropertyNotFoundException;
import com.propstack.property.persistence.PropertyStatus;
import com.propstack.property.persistence.PropertyType;
import com.propstack.property.service.PropertyService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.security.oauth2.server.resource.autoconfigure.web.OAuth2ResourceServerWebSecurityAutoConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

/**
 * Security filters are disabled here (addFilters = false) so this slice test can focus purely on
 * request mapping, validation, and response shape.
 */
@WebMvcTest(controllers = PropertyController.class, excludeAutoConfiguration = OAuth2ResourceServerWebSecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class PropertyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PropertyService propertyService;

    @Test
    void findAll_returnsOkWithPropertyList() throws Exception {
        PropertyResponse response = PropertyResponse.builder().id(1L).name("Sunset Apartments").build();
        when(propertyService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Sunset Apartments"));
    }

    @Test
    void findById_returnsNotFound_whenServiceThrows() throws Exception {
        when(propertyService.findById(404L)).thenThrow(new PropertyNotFoundException(404L));

        mockMvc.perform(get("/api/properties/404"))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreatedWithLocationHeader() throws Exception {
        PropertyRequest request = PropertyRequest.builder()
                .name("Skyline Tower")
                .address(AddressRequest.builder().street("500 Harbor Blvd").city("Baytown").build())
                .type(PropertyType.BUILDING)
                .status(PropertyStatus.AVAILABLE)
                .build();
        PropertyResponse response = PropertyResponse.builder()
                .id(9L)
                .name("Skyline Tower")
                .address(AddressResponse.builder().street("500 Harbor Blvd").city("Baytown").build())
                .type(PropertyType.BUILDING)
                .status(PropertyStatus.AVAILABLE)
                .build();
        when(propertyService.create(any(PropertyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/properties/9")))
                .andExpect(jsonPath("$.name").value("Skyline Tower"));
    }

    @Test
    void create_returnsBadRequest_whenNameMissing() throws Exception {
        PropertyRequest invalidRequest = PropertyRequest.builder()
                .address(AddressRequest.builder().street("500 Harbor Blvd").city("Baytown").build())
                .type(PropertyType.BUILDING)
                .status(PropertyStatus.AVAILABLE)
                .build();

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void create_returnsBadRequest_whenAddressMissing() throws Exception {
        PropertyRequest invalidRequest = PropertyRequest.builder()
                .name("Skyline Tower")
                .type(PropertyType.BUILDING)
                .status(PropertyStatus.AVAILABLE)
                .build();

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void update_returnsOkWithUpdatedProperty() throws Exception {
        PropertyRequest request = PropertyRequest.builder()
                .name("Renamed")
                .address(AddressRequest.builder().street("1 Main St").city("Springfield").build())
                .type(PropertyType.HOUSE)
                .status(PropertyStatus.SOLD)
                .build();
        PropertyResponse response = PropertyResponse.builder().id(3L).name("Renamed").build();
        when(propertyService.update(eq(3L), any(PropertyRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/properties/3")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/properties/3"))
                .andExpect(status().isNoContent());

        verify(propertyService).delete(3L);
    }
}
