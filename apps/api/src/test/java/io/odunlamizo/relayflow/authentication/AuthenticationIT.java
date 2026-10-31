package io.odunlamizo.relayflow.authentication;

// Docs: docs/codebase-components.md#backend-tests

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.odunlamizo.relayflow.support.ApplicationIntegrationTest;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

class AuthenticationIT extends ApplicationIntegrationTest {

    @Autowired private MockMvc mvc;
    @Autowired private PasswordEncoder encoder;

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void bootstrapCreatesOwnerAndSessionAndCannotRunTwice() throws Exception {
        mvc.perform(get("/auth/bootstrap-status"))
                .andExpect(jsonPath("$.bootstrapped").value(false));

        var result =
                mvc.perform(
                                post("/auth/bootstrap")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                """
                                {"email":"owner@example.com","name":"Owner",
                                 "password":"Strong-password-123!","workspaceName":"My workspace"}
                                """))
                        .andExpect(status().isOk())
                        .andReturn();
        flushAndClear();

        assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
        assertThat(jdbc.queryForObject("select role from workspace_members", String.class))
                .isEqualTo("OWNER");
        String hash = jdbc.queryForObject("select credential from user_identities", String.class);
        assertThat(encoder.matches("Strong-password-123!", hash)).isTrue();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("owner@example.com"));
        mvc.perform(
                        post("/auth/bootstrap")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(
                                        """
                                {"email":"another@example.com","name":"Another",
                                 "password":"Strong-password-123!","workspaceName":"Another workspace"}
                                """))
                .andExpect(status().isConflict());
        assertThat(jdbc.queryForObject("select count(*) from users", Integer.class)).isEqualTo(1);
    }

    @Test
    void loginSessionSurvivesRequestsAndLogoutInvalidatesIt() throws Exception {
        UUID id = passwordUser();
        String email = id + "@example.com";
        var result =
                mvc.perform(
                                post("/auth/login")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(
                                                "{\"email\":\""
                                                        + email
                                                        + "\",\"password\":\"Old-password-123!\"}"))
                        .andExpect(status().isOk())
                        .andReturn();
        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertThat(session).isNotNull();
        mvc.perform(get("/auth/me").session(session))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value(email));
        mvc.perform(post("/auth/logout").session(session)).andExpect(status().isNoContent());
        assertThat(session.isInvalid()).isTrue();
    }

    @Test
    void passwordResetChangesCredentialAndTokenCannotBeReused() throws Exception {
        UUID id = passwordUser();
        UUID token = resetToken(id, false);
        reset(token, "New-password-123!").andExpect(status().isNoContent());
        flushAndClear();
        String hash =
                jdbc.queryForObject(
                        "select credential from user_identities where user_id = ?",
                        String.class,
                        id);
        assertThat(encoder.matches("New-password-123!", hash)).isTrue();
        assertThat(encoder.matches("Old-password-123!", hash)).isFalse();
        assertThat(
                        jdbc.queryForObject(
                                "select used_at is not null from password_reset_tokens where token = ?",
                                Boolean.class,
                                token))
                .isTrue();
        reset(token, "Another-password-123!").andExpect(status().isBadRequest());
    }

    @Test
    void expiredResetTokenCannotChangePassword() throws Exception {
        UUID id = passwordUser();
        UUID token = resetToken(id, true);
        reset(token, "New-password-123!").andExpect(status().isBadRequest());
        flushAndClear();
        String hash =
                jdbc.queryForObject(
                        "select credential from user_identities where user_id = ?",
                        String.class,
                        id);
        assertThat(encoder.matches("Old-password-123!", hash)).isTrue();
        assertThat(
                        jdbc.queryForObject(
                                "select used_at is null from password_reset_tokens where token = ?",
                                Boolean.class,
                                token))
                .isTrue();
    }

    private UUID passwordUser() {
        UUID id = user();
        jdbc.update(
                "insert into user_identities (user_id, provider, provider_subject, credential, verified)"
                        + " values (?, 'EMAIL', ?, ?, true)",
                id,
                id + "@example.com",
                encoder.encode("Old-password-123!"));
        return id;
    }

    private UUID resetToken(UUID userId, boolean expired) {
        UUID token = UUID.randomUUID();
        jdbc.update(
                "insert into password_reset_tokens (user_id, token, expires_at)"
                        + " values (?, ?, now() + (? * interval '1 hour'))",
                userId,
                token,
                expired ? -1 : 1);
        return token;
    }

    private org.springframework.test.web.servlet.ResultActions reset(UUID token, String password)
            throws Exception {
        return mvc.perform(
                post("/auth/reset-password/" + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"newPassword\":\"" + password + "\"}"));
    }
}
