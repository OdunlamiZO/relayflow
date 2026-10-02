package com.relayflow.api.contact;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.relayflow.api.contact.domain.WhitelistedPhoneNumber;
import com.relayflow.api.contact.dto.WhitelistedPhoneNumberResponse;
import com.relayflow.api.contact.repository.WhitelistedPhoneNumberRepository;
import com.relayflow.api.workspace.domain.ContactAccess;
import com.relayflow.api.workspace.domain.Workspace;
import com.relayflow.api.workspace.repository.WorkspaceRepository;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class ContactAccessServiceTest {

    @Mock private WhitelistedPhoneNumberRepository whitelistRepository;

    @Mock private WorkspaceRepository workspaceRepository;

    private ContactAccessService service;

    private Workspace workspace;

    @BeforeEach
    void setUp() {
        service =
                new ContactAccessService(
                        whitelistRepository, new PhoneNumberNormalizer(), workspaceRepository);
        workspace = new Workspace();
        workspace.setId(UUID.randomUUID());
        workspace.setPhoneRegion("NG");
    }

    @Test
    void allowsEveryoneWhenNotRestricted() {
        assertThat(service.isAllowed(workspace, "+447700900123")).isTrue();
        verify(whitelistRepository, never()).existsByWorkspaceAndPhoneNumber(any(), any());
    }

    @Test
    void checksTheNormalizedNumberWhenRestricted() {
        workspace.setContactAccess(ContactAccess.WHITELIST);
        when(whitelistRepository.existsByWorkspaceAndPhoneNumber(
                        workspace.getId(), "+2348031234567"))
                .thenReturn(true);

        assertThat(service.isAllowed(workspace, "08031234567")).isTrue();
    }

    @Test
    void blocksAnInvalidNumberWhenRestricted() {
        workspace.setContactAccess(ContactAccess.WHITELIST);

        assertThat(service.isAllowed(workspace, "unknown")).isFalse();
    }

    @Test
    void addsOnlyNumbersNotAlreadyWhitelisted() {
        when(whitelistRepository.findExisting(any(), anyCollection()))
                .thenReturn(List.of("+2348031234567"));
        when(whitelistRepository.save(any()))
                .thenAnswer(
                        invocation -> {
                            WhitelistedPhoneNumber entry = invocation.getArgument(0);
                            entry.setId(UUID.randomUUID());

                            return entry;
                        });

        List<WhitelistedPhoneNumberResponse> added =
                service.addToWhitelist(
                        workspace, List.of("08031234567", "+2348039876543", "08039876543"));

        assertThat(added)
                .extracting(WhitelistedPhoneNumberResponse::phoneNumber)
                .containsExactly("+2348039876543");
    }

    @Test
    void rejectsTheBatchWhenAnyNumberIsInvalid() {
        assertThatThrownBy(
                        () -> service.addToWhitelist(workspace, List.of("+2348031234567", "abc")))
                .isInstanceOf(ResponseStatusException.class)
                .hasMessageContaining("abc");
        verify(whitelistRepository, never()).save(any());
    }

    @Test
    void rejectsAnUnknownRegion() {
        assertThatThrownBy(() -> service.updateSettings(workspace, ContactAccess.WHITELIST, "XX"))
                .isInstanceOf(ResponseStatusException.class);
    }

    @Test
    void savesTheModeAndUpperCasedRegion() {
        when(workspaceRepository.save(workspace)).thenReturn(workspace);

        service.updateSettings(workspace, ContactAccess.WHITELIST, "gb");

        assertThat(workspace.getContactAccess()).isEqualTo(ContactAccess.WHITELIST);
        assertThat(workspace.getPhoneRegion()).isEqualTo("GB");
    }
}
