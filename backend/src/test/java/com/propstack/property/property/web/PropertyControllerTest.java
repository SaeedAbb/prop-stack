package com.propstack.property.property.web;

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

import com.propstack.property.address.dto.AddressRequest;
import com.propstack.property.address.dto.AddressResponse;
import com.propstack.property.property.dto.PropertyRequest;
import com.propstack.property.property.dto.PropertyResponse;
import com.propstack.property.property.exception.PropertyNotFoundException;
import com.propstack.property.property.persistence.PropertyStatus;
import com.propstack.property.property.persistence.PropertyType;
import com.propstack.property.property.service.PropertyService;
import java.util.List;
import java.util.UUID;
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

    private static final UUID PROPERTY_ID_1 = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final UUID PROPERTY_ID_2 = UUID.fromString("00000000-0000-0000-0000-000000000002");
    private static final UUID PROPERTY_ID_3 = UUID.fromString("00000000-0000-0000-0000-000000000003");
    private static final UUID PROPERTY_ID_4 = UUID.fromString("00000000-0000-0000-0000-000000000004");
    private static final UUID PROPERTY_ID_9 = UUID.fromString("00000000-0000-0000-0000-000000000009");
    private static final UUID PROPERTY_ID_404 = UUID.fromString("00000000-0000-0000-0000-000000000404");

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private PropertyService propertyService;

    @Test
    void findAll_returnsOkWithPropertyList() throws Exception {
        PropertyResponse response = PropertyResponse.builder().id(PROPERTY_ID_1).name("Sunset Apartments").build();
        when(propertyService.findAll()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/properties"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Sunset Apartments"));
    }

    @Test
    void findAllDeleted_returnsOkWithPropertyList() throws Exception {
        PropertyResponse response = PropertyResponse.builder().id(PROPERTY_ID_2).name("Old Warehouse").build();
        when(propertyService.findAllDeleted()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/properties/deleted"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].name").value("Old Warehouse"));
    }

    @Test
    void findById_returnsNotFound_whenServiceThrows() throws Exception {
        when(propertyService.findById(PROPERTY_ID_404)).thenThrow(new PropertyNotFoundException(PROPERTY_ID_404));

        mockMvc.perform(get("/api/properties/" + PROPERTY_ID_404))
                .andExpect(status().isNotFound());
    }

    @Test
    void create_returnsCreatedWithLocationHeader() throws Exception {
        PropertyRequest request = PropertyRequest.builder()
                .name("Skyline Tower")
                .address(validAddressRequest())
                .type(PropertyType.BUILDING)
                .status(PropertyStatus.AVAILABLE)
                .build();
        PropertyResponse response = PropertyResponse.builder()
                .id(PROPERTY_ID_9)
                .name("Skyline Tower")
                .address(AddressResponse.builder().street("Harbor Blvd").houseNumber("500").city("Baytown").build())
                .type(PropertyType.BUILDING)
                .status(PropertyStatus.AVAILABLE)
                .build();
        when(propertyService.create(any(PropertyRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/properties")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", containsString("/api/properties/" + PROPERTY_ID_9)))
                .andExpect(jsonPath("$.name").value("Skyline Tower"));
    }

    @Test
    void create_returnsBadRequest_whenNameMissing() throws Exception {
        PropertyRequest invalidRequest = PropertyRequest.builder()
                .address(validAddressRequest())
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
    void create_returnsBadRequest_whenHouseNumberMissing() throws Exception {
        PropertyRequest invalidRequest = PropertyRequest.builder()
                .name("Skyline Tower")
                .address(AddressRequest.builder()
                        .street("Harbor Blvd")
                        .city("Baytown")
                        .state("CA")
                        .postalCode("94001")
                        .country("USA")
                        .build())
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
                .address(validAddressRequest())
                .type(PropertyType.HOUSE)
                .status(PropertyStatus.SOLD)
                .build();
        PropertyResponse response = PropertyResponse.builder().id(PROPERTY_ID_3).name("Renamed").build();
        when(propertyService.update(eq(PROPERTY_ID_3), any(PropertyRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/properties/" + PROPERTY_ID_3)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Renamed"));
    }

    @Test
    void delete_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/properties/" + PROPERTY_ID_3))
                .andExpect(status().isNoContent());

        verify(propertyService).delete(PROPERTY_ID_3);
    }

    @Test
    void restore_returnsOkWithRestoredProperty() throws Exception {
        PropertyResponse response = PropertyResponse.builder().id(PROPERTY_ID_4).name("Old Warehouse").build();
        when(propertyService.restore(PROPERTY_ID_4)).thenReturn(response);

        mockMvc.perform(post("/api/properties/" + PROPERTY_ID_4 + "/restore"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Old Warehouse"));
    }

    @Test
    void restore_returnsNotFound_whenServiceThrows() throws Exception {
        when(propertyService.restore(PROPERTY_ID_404)).thenThrow(new PropertyNotFoundException(PROPERTY_ID_404));

        mockMvc.perform(post("/api/properties/" + PROPERTY_ID_404 + "/restore"))
                .andExpect(status().isNotFound());
    }

    private static AddressRequest validAddressRequest() {
        return AddressRequest.builder()
                .street("Harbor Blvd")
                .houseNumber("500")
                .city("Baytown")
                .state("CA")
                .postalCode("94001")
                .country("USA")
                .build();
    }
}
