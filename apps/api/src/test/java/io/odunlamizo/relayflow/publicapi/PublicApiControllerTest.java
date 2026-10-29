package io.odunlamizo.relayflow.publicapi;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import io.odunlamizo.relayflow.authentication.ApiKeyAuthentication;
import io.odunlamizo.relayflow.contact.ContactNotFoundException;
import io.odunlamizo.relayflow.contact.ContactService;
import io.odunlamizo.relayflow.contact.InvalidContactTagValueException;
import io.odunlamizo.relayflow.contact.UnknownContactTagException;
import io.odunlamizo.relayflow.messaging.ConversationLockedException;
import io.odunlamizo.relayflow.messaging.ConversationNotFoundException;
import io.odunlamizo.relayflow.messaging.MessagingService;
import io.odunlamizo.relayflow.telegram.TelegramSendException;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PublicApiController.class)
@AutoConfigureMockMvc(addFilters = false)
@Import({PublicApiExceptionResolver.class, PublicApiProblemWriter.class})
class PublicApiControllerTest {

    private static final UUID WORKSPACE_ID = UUID.randomUUID();

    private static final ApiKeyAuthentication API_KEY = new ApiKeyAuthentication(WORKSPACE_ID);

    @Autowired private MockMvc mockMvc;

    @MockBean private MessagingService messagingService;

    @MockBean private ContactService contactService;

    @Test
    void unknownConversationIsAProblem() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(messagingService.getConversation(WORKSPACE_ID, conversationId))
                .thenThrow(new ConversationNotFoundException());

        mockMvc.perform(get("/public/v1/conversations/" + conversationId).principal(API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.type").value("/problems/conversation-not-found"))
                .andExpect(jsonPath("$.title").value("Conversation not found"))
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.detail").value("Conversation not found"))
                .andExpect(
                        jsonPath("$.instance").value("/public/v1/conversations/" + conversationId))
                .andExpect(jsonPath("$.code").value("CONVERSATION_NOT_FOUND"));
    }

    @Test
    void unknownContactIsAProblem() throws Exception {
        UUID contactId = UUID.randomUUID();
        when(contactService.updateContactTags(eq(contactId), eq(WORKSPACE_ID), anyMap()))
                .thenThrow(new ContactNotFoundException());

        mockMvc.perform(
                        put("/public/v1/contacts/" + contactId + "/tags")
                                .principal(API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"kyc_status\":\"verified\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("CONTACT_NOT_FOUND"));
    }

    @Test
    void unknownTagIsAProblem() throws Exception {
        UUID contactId = UUID.randomUUID();
        when(contactService.updateContactTags(eq(contactId), eq(WORKSPACE_ID), anyMap()))
                .thenThrow(new UnknownContactTagException("tier"));

        mockMvc.perform(
                        put("/public/v1/contacts/" + contactId + "/tags")
                                .principal(API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"tier\":\"gold\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("UNKNOWN_TAG"))
                .andExpect(jsonPath("$.detail").value("\"tier\" is not a contact tag."));
    }

    @Test
    void disallowedTagValueIsAProblem() throws Exception {
        UUID contactId = UUID.randomUUID();
        when(contactService.updateContactTags(eq(contactId), eq(WORKSPACE_ID), anyMap()))
                .thenThrow(
                        new InvalidContactTagValueException(
                                "kyc_status", "done", List.of("pending", "verified")));

        mockMvc.perform(
                        put("/public/v1/contacts/" + contactId + "/tags")
                                .principal(API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"kyc_status\":\"done\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_TAG_VALUE"))
                .andExpect(
                        jsonPath("$.detail")
                                .value(
                                        "\"done\" is not an allowed value for \"kyc_status\"."
                                                + " Allowed values: pending, verified."));
    }

    @Test
    void lockedConversationIsAProblem() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(messagingService.createMessage(
                        eq(WORKSPACE_ID), eq(conversationId), any(), isNull(), anyBoolean()))
                .thenThrow(new ConversationLockedException("A workflow is handling it."));

        mockMvc.perform(
                        post("/public/v1/conversations/" + conversationId + "/messages")
                                .principal(API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\"Hello\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("CONVERSATION_LOCKED"));
    }

    @Test
    void channelFailureIsABadGateway() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(messagingService.createMessage(
                        eq(WORKSPACE_ID), eq(conversationId), any(), isNull(), anyBoolean()))
                .thenThrow(new TelegramSendException("Telegram channel is disconnected", null));

        mockMvc.perform(
                        post("/public/v1/conversations/" + conversationId + "/messages")
                                .principal(API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\"Hello\"}"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("CHANNEL_DELIVERY_FAILED"))
                .andExpect(jsonPath("$.detail").value("Telegram channel is disconnected"));
    }

    @Test
    void blankTextIsAnInvalidRequest() throws Exception {
        mockMvc.perform(
                        post("/public/v1/conversations/" + UUID.randomUUID() + "/messages")
                                .principal(API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(jsonPath("$.detail").value("text: must not be blank"));
    }

    @Test
    void malformedJsonIsAnInvalidRequest() throws Exception {
        mockMvc.perform(
                        post("/public/v1/conversations/" + UUID.randomUUID() + "/messages")
                                .principal(API_KEY)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content("{\"text\":"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(
                        jsonPath("$.detail")
                                .value("The request body is missing or isn't valid JSON."));
    }

    @Test
    void malformedIdIsAnInvalidRequest() throws Exception {
        mockMvc.perform(get("/public/v1/conversations/not-a-uuid").principal(API_KEY))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("INVALID_REQUEST"))
                .andExpect(
                        jsonPath("$.detail").value("\"not-a-uuid\" isn't a valid conversationId."));
    }

    @Test
    void unknownEndpointIsAProblem() throws Exception {
        mockMvc.perform(get("/public/v1/invoices").principal(API_KEY))
                .andExpect(status().isNotFound())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_PROBLEM_JSON))
                .andExpect(jsonPath("$.code").value("ENDPOINT_NOT_FOUND"));
    }

    @Test
    void unsupportedMethodIsAProblem() throws Exception {
        mockMvc.perform(delete("/public/v1/conversations").principal(API_KEY))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(header().string("Allow", "GET"))
                .andExpect(jsonPath("$.code").value("METHOD_NOT_ALLOWED"));
    }

    @Test
    void nonJsonBodyIsAProblem() throws Exception {
        mockMvc.perform(
                        post("/public/v1/conversations/" + UUID.randomUUID() + "/messages")
                                .principal(API_KEY)
                                .contentType(MediaType.TEXT_PLAIN)
                                .content("Hello"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.code").value("UNSUPPORTED_MEDIA_TYPE"));
    }

    @Test
    void unexpectedFailureHidesItsCause() throws Exception {
        UUID conversationId = UUID.randomUUID();
        when(messagingService.getConversation(WORKSPACE_ID, conversationId))
                .thenThrow(new IllegalStateException("database password is wrong"));

        mockMvc.perform(get("/public/v1/conversations/" + conversationId).principal(API_KEY))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.code").value("INTERNAL_ERROR"))
                .andExpect(jsonPath("$.detail").value("An unexpected error occurred."));
    }
}
