package com.relayflow.api.contact;

import com.relayflow.api.contact.dto.AddWhitelistedPhoneNumbersRequest;
import com.relayflow.api.contact.dto.WhitelistedPhoneNumberResponse;
import com.relayflow.api.workspace.WorkspaceAuthorizationService;
import com.relayflow.api.workspace.WorkspaceMapper;
import com.relayflow.api.workspace.WorkspaceService;
import com.relayflow.api.workspace.dto.UpdateContactAccessRequest;
import com.relayflow.api.workspace.dto.WorkspaceResponse;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/workspaces/{workspaceId}")
public class ContactAccessController {

    private final ContactAccessService contactAccessService;

    private final WorkspaceService workspaceService;

    private final WorkspaceMapper workspaceMapper;

    private final WorkspaceAuthorizationService authorizationService;

    public ContactAccessController(
            ContactAccessService contactAccessService,
            WorkspaceService workspaceService,
            WorkspaceMapper workspaceMapper,
            WorkspaceAuthorizationService authorizationService) {
        this.contactAccessService = contactAccessService;
        this.workspaceService = workspaceService;
        this.workspaceMapper = workspaceMapper;
        this.authorizationService = authorizationService;
    }

    @PutMapping("/contact-access")
    WorkspaceResponse updateContactAccess(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody UpdateContactAccessRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return workspaceMapper.toDto(
                contactAccessService.updateSettings(
                        workspaceService.getWorkspace(workspaceId),
                        request.contactAccess(),
                        request.phoneRegion()));
    }

    @GetMapping("/whitelist")
    List<WhitelistedPhoneNumberResponse> listWhitelist(
            @PathVariable UUID workspaceId, Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return contactAccessService.listWhitelist(workspaceId);
    }

    @PostMapping("/whitelist")
    @ResponseStatus(HttpStatus.CREATED)
    List<WhitelistedPhoneNumberResponse> addToWhitelist(
            @PathVariable UUID workspaceId,
            @Valid @RequestBody AddWhitelistedPhoneNumbersRequest request,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        return contactAccessService.addToWhitelist(
                workspaceService.getWorkspace(workspaceId), request.phoneNumbers());
    }

    @DeleteMapping("/whitelist/{entryId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void removeFromWhitelist(
            @PathVariable UUID workspaceId,
            @PathVariable UUID entryId,
            Authentication authentication) {
        authorizationService.assertOwner(workspaceId, authentication);

        contactAccessService.removeFromWhitelist(workspaceId, entryId);
    }
}
