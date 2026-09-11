package com.relayflow.api.contact;

import com.relayflow.api.common.dto.PageResponse;
import com.relayflow.api.contact.dto.ContactDetailResponse;
import com.relayflow.api.contact.dto.ContactResponse;
import com.relayflow.api.contact.dto.CreateContactRequest;
import com.relayflow.api.contact.dto.CreateExternalIdentityRequest;
import com.relayflow.api.contact.dto.ExternalIdentityResponse;
import com.relayflow.api.contact.dto.MergeContactRequest;
import com.relayflow.api.contact.dto.UpdateContactCustomFieldsRequest;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import com.relayflow.api.workspace.domain.WorkspacePermission;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
public class ContactController {

    private final ContactService contactService;
    private final WorkspaceAuthorizationService authorizationService;

    public ContactController(
            ContactService contactService, WorkspaceAuthorizationService authorizationService) {
        this.contactService = contactService;
        this.authorizationService = authorizationService;
    }

    // --- Contacts ---

    @GetMapping("/contacts")
    PageResponse<ContactResponse> listContacts(
            @RequestParam @NotNull UUID workspaceId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        return contactService.listContacts(workspaceId, page, size);
    }

    @PostMapping("/contacts")
    @ResponseStatus(HttpStatus.CREATED)
    ContactResponse createContact(@Valid @RequestBody CreateContactRequest request) {
        return contactService.createContact(request);
    }

    @GetMapping("/contacts/{id}")
    ContactDetailResponse getContact(
            @PathVariable UUID id, @RequestParam @NotNull UUID workspaceId) {
        return contactService.getContactDetail(id, workspaceId);
    }

    @PatchMapping("/contacts/{id}/custom-fields")
    ContactDetailResponse updateContactCustomFields(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            @Valid @RequestBody UpdateContactCustomFieldsRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CONTACT_FIELDS_WRITE);

        return contactService.updateContactCustomFields(id, workspaceId, request.customFields());
    }

    @PostMapping("/contacts/{id}/merge")
    ContactResponse mergeContacts(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            @Valid @RequestBody MergeContactRequest request,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CONTACTS_DELETE);

        return contactService.mergeContacts(id, request.sourceContactId(), workspaceId);
    }

    @DeleteMapping("/contacts/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void deleteContact(
            @PathVariable UUID id,
            @RequestParam @NotNull UUID workspaceId,
            Authentication authentication) {
        authorizationService.assertPermission(
                workspaceId, authentication, WorkspacePermission.CONTACTS_DELETE);

        contactService.deleteContact(id, workspaceId);
    }

    // --- External Identities ---

    @PostMapping("/external-identities")
    @ResponseStatus(HttpStatus.CREATED)
    ExternalIdentityResponse createExternalIdentity(
            @Valid @RequestBody CreateExternalIdentityRequest request) {
        return contactService.createExternalIdentity(request);
    }
}
