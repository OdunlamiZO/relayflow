package com.relayflow.api.contact;

import com.relayflow.api.common.ResourceNotFoundException;
import com.relayflow.api.contact.domain.WhitelistedPhoneNumber;
import com.relayflow.api.contact.dto.WhitelistedPhoneNumberResponse;
import com.relayflow.api.contact.repository.WhitelistedPhoneNumberRepository;
import com.relayflow.api.workspace.domain.ContactAccess;
import com.relayflow.api.workspace.domain.Workspace;
import com.relayflow.api.workspace.repository.WorkspaceRepository;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ContactAccessService {

    private final WhitelistedPhoneNumberRepository whitelistRepository;

    private final PhoneNumberNormalizer phoneNumberNormalizer;

    private final WorkspaceRepository workspaceRepository;

    public ContactAccessService(
            WhitelistedPhoneNumberRepository whitelistRepository,
            PhoneNumberNormalizer phoneNumberNormalizer,
            WorkspaceRepository workspaceRepository) {
        this.whitelistRepository = whitelistRepository;
        this.phoneNumberNormalizer = phoneNumberNormalizer;
        this.workspaceRepository = workspaceRepository;
    }

    public boolean isRestricted(Workspace workspace) {
        return workspace.getContactAccess() == ContactAccess.WHITELIST;
    }

    @Transactional(readOnly = true)
    public boolean isAllowed(Workspace workspace, String phoneNumber) {
        if (!isRestricted(workspace)) {
            return true;
        }

        return normalize(workspace, phoneNumber)
                .map(
                        normalized ->
                                whitelistRepository.existsByWorkspaceAndPhoneNumber(
                                        workspace.getId(), normalized))
                .orElse(false);
    }

    public Optional<String> normalize(Workspace workspace, String phoneNumber) {
        return phoneNumberNormalizer.toE164(phoneNumber, workspace.getPhoneRegion());
    }

    @Transactional(readOnly = true)
    public List<WhitelistedPhoneNumberResponse> listWhitelist(UUID workspaceId) {
        return whitelistRepository.findByWorkspace(workspaceId).stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public List<WhitelistedPhoneNumberResponse> addToWhitelist(
            Workspace workspace, List<String> phoneNumbers) {
        Set<String> normalized = new LinkedHashSet<>();
        List<String> invalid = new ArrayList<>();

        for (String phoneNumber : phoneNumbers) {
            normalize(workspace, phoneNumber)
                    .ifPresentOrElse(normalized::add, () -> invalid.add(phoneNumber));
        }

        if (!invalid.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Not valid phone numbers: "
                            + String.join(", ", invalid)
                            + ". Use the international format (e.g. +14155550123) or set a"
                            + " default country.");
        }

        normalized.removeAll(whitelistRepository.findExisting(workspace.getId(), normalized));

        return normalized.stream()
                .map(
                        phoneNumber -> {
                            WhitelistedPhoneNumber entry = new WhitelistedPhoneNumber();
                            entry.setWorkspaceId(workspace.getId());
                            entry.setPhoneNumber(phoneNumber);

                            return toResponse(whitelistRepository.save(entry));
                        })
                .toList();
    }

    @Transactional
    public void removeFromWhitelist(UUID workspaceId, UUID entryId) {
        WhitelistedPhoneNumber entry =
                whitelistRepository
                        .findInWorkspace(entryId, workspaceId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Whitelisted phone number not found"));

        whitelistRepository.delete(entry);
    }

    @Transactional
    public Workspace updateSettings(
            Workspace workspace, ContactAccess contactAccess, String phoneRegion) {
        String region =
                phoneRegion != null && !phoneRegion.isBlank()
                        ? phoneRegion.trim().toUpperCase()
                        : null;

        if (region != null && !phoneNumberNormalizer.isSupportedRegion(region)) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "\"" + region + "\" is not a two-letter country code like US or GB.");
        }

        workspace.setContactAccess(contactAccess);
        workspace.setPhoneRegion(region);

        return workspaceRepository.save(workspace);
    }

    private WhitelistedPhoneNumberResponse toResponse(WhitelistedPhoneNumber entry) {
        return new WhitelistedPhoneNumberResponse(
                entry.getId(), entry.getPhoneNumber(), entry.getCreatedAt());
    }
}
