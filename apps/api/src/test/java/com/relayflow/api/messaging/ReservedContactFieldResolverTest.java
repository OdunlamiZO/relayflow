package com.relayflow.api.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import com.relayflow.api.messaging.domain.ChannelProvider;
import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.messaging.domain.ExternalIdentity;
import java.util.List;
import org.junit.jupiter.api.Test;

class ReservedContactFieldResolverTest {

    private final ReservedContactFieldResolver resolver = new ReservedContactFieldResolver();

    private ExternalIdentity identity(ChannelProvider provider, String externalUserId) {
        ExternalIdentity identity = new ExternalIdentity();
        identity.setProvider(provider);
        identity.setExternalUserId(externalUserId);

        return identity;
    }

    @Test
    void resolvesNameFromDisplayName() {
        Contact contact = new Contact();
        contact.setDisplayName("Ada Lovelace");

        var fields = resolver.resolve(contact, List.of());

        assertThat(fields).containsEntry("displayName", "Ada Lovelace");
    }

    @Test
    void omitsNameWhenDisplayNameIsBlank() {
        Contact contact = new Contact();
        contact.setDisplayName(null);

        var fields = resolver.resolve(contact, List.of());

        assertThat(fields).doesNotContainKey("displayName");
    }

    @Test
    void resolvesPhoneAndCountryFromAWhatsAppIdentity() {
        Contact contact = new Contact();
        List<ExternalIdentity> identities =
                List.of(identity(ChannelProvider.WHATSAPP, "2348012345678"));

        var fields = resolver.resolve(contact, identities);

        assertThat(fields).containsEntry("phone", "2348012345678");
        assertThat(fields).containsEntry("country", "Nigeria");
    }

    @Test
    void resolvesPhoneFromAnSmsIdentityWhenNoWhatsApp() {
        Contact contact = new Contact();
        List<ExternalIdentity> identities = List.of(identity(ChannelProvider.SMS, "14155552671"));

        var fields = resolver.resolve(contact, identities);

        assertThat(fields).containsEntry("phone", "14155552671");
        assertThat(fields).containsEntry("country", "United States");
    }

    @Test
    void resolvesEmailFromAnEmailIdentity() {
        Contact contact = new Contact();
        List<ExternalIdentity> identities =
                List.of(identity(ChannelProvider.EMAIL, "ada@example.com"));

        var fields = resolver.resolve(contact, identities);

        assertThat(fields).containsEntry("email", "ada@example.com");
    }

    @Test
    void ignoresIdentitiesFromNonPhoneNonEmailProviders() {
        Contact contact = new Contact();
        List<ExternalIdentity> identities =
                List.of(identity(ChannelProvider.TELEGRAM, "123456789"));

        var fields = resolver.resolve(contact, identities);

        assertThat(fields).doesNotContainKeys("phone", "email", "country");
    }

    @Test
    void omitsCountryWhenThePhoneNumberCannotBeParsed() {
        Contact contact = new Contact();
        List<ExternalIdentity> identities =
                List.of(identity(ChannelProvider.WHATSAPP, "not-a-number"));

        var fields = resolver.resolve(contact, identities);

        assertThat(fields).containsEntry("phone", "not-a-number");
        assertThat(fields).doesNotContainKey("country");
    }
}
