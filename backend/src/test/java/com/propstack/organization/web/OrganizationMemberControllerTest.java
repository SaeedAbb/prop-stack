package com.propstack.organization.web;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.propstack.organization.dto.AddOrganizationMemberRequest;
import com.propstack.organization.dto.OrganizationMemberResponse;
import com.propstack.organization.service.OrganizationAdminService;
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
 * request mapping, validation, and response shape - {@code @PreAuthorize("hasRole('ORG-ADMIN')")}
 * on this controller is not exercised in this test class.
 */
@WebMvcTest(controllers = OrganizationMemberController.class, excludeAutoConfiguration = OAuth2ResourceServerWebSecurityAutoConfiguration.class)
@AutoConfigureMockMvc(addFilters = false)
class OrganizationMemberControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private OrganizationAdminService organizationAdminService;

    @Test
    void list_returnsOkWithMembers() throws Exception {
        OrganizationMemberResponse response = OrganizationMemberResponse.builder().id("u1").email("jdoe@example.com").build();
        when(organizationAdminService.listMembers()).thenReturn(List.of(response));

        mockMvc.perform(get("/api/organization/members"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].email").value("jdoe@example.com"));
    }

    @Test
    void add_returnsCreated_whenRequestValid() throws Exception {
        AddOrganizationMemberRequest request = AddOrganizationMemberRequest.builder().email("new@example.com").build();

        mockMvc.perform(post("/api/organization/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        verify(organizationAdminService).addMember(any(AddOrganizationMemberRequest.class));
    }

    @Test
    void add_returnsBadRequest_whenEmailInvalid() throws Exception {
        AddOrganizationMemberRequest request = AddOrganizationMemberRequest.builder().email("not-an-email").build();

        mockMvc.perform(post("/api/organization/members")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void remove_returnsNoContent() throws Exception {
        mockMvc.perform(delete("/api/organization/members/u1"))
                .andExpect(status().isNoContent());

        verify(organizationAdminService).removeMember("u1");
    }
}
