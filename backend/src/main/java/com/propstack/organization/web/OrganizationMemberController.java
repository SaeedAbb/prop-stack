package com.propstack.organization;

import com.propstack.organization.dto.AddOrganizationMemberRequest;
import com.propstack.organization.dto.OrganizationMemberResponse;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/organization/members")
@PreAuthorize("hasRole('ORG-ADMIN')")
public class OrganizationMemberController {

    private final OrganizationAdminService organizationAdminService;

    public OrganizationMemberController(OrganizationAdminService organizationAdminService) {
        this.organizationAdminService = organizationAdminService;
    }

    @GetMapping
    public List<OrganizationMemberResponse> list() {
        return organizationAdminService.listMembers();
    }

    @PostMapping
    public ResponseEntity<Void> add(@Valid @RequestBody AddOrganizationMemberRequest request) {
        organizationAdminService.addMember(request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<Void> remove(@PathVariable String userId) {
        organizationAdminService.removeMember(userId);
        return ResponseEntity.noContent().build();
    }
}
