package com.relayflow.api.messaging;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import com.relayflow.api.messaging.domain.ChannelProvider;
import com.relayflow.api.messaging.domain.Contact;
import com.relayflow.api.messaging.domain.ExternalIdentity;
import com.relayflow.api.messaging.domain.ReservedContactField;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * Resolves the fields RelayFlow can already derive from existing data — without needing the AI
 * agent to ask for them or an operator to enter them manually. See {@link ReservedContactField} for
 * the full set of reserved keys.
 */
@Component
public class ReservedContactFieldResolver {

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public Map<String, String> resolve(Contact contact, List<ExternalIdentity> identities) {
        Map<String, String> fields = new LinkedHashMap<>();

        if (contact.getDisplayName() != null && !contact.getDisplayName().isBlank()) {
            fields.put(ReservedContactField.DISPLAY_NAME.key(), contact.getDisplayName());
        }

        String phone = findIdentifier(identities, ChannelProvider.WHATSAPP, ChannelProvider.SMS);
        if (phone != null) {
            fields.put(ReservedContactField.PHONE.key(), phone);
            resolveCountry(phone)
                    .ifPresent(country -> fields.put(ReservedContactField.COUNTRY.key(), country));
        }

        String email = findIdentifier(identities, ChannelProvider.EMAIL);
        if (email != null) {
            fields.put(ReservedContactField.EMAIL.key(), email);
        }

        return fields;
    }

    private String findIdentifier(List<ExternalIdentity> identities, ChannelProvider... providers) {
        Set<ChannelProvider> wanted = Set.of(providers);

        return identities.stream()
                .filter(identity -> wanted.contains(identity.getProvider()))
                .map(ExternalIdentity::getExternalUserId)
                .filter(id -> id != null && !id.isBlank())
                .findFirst()
                .orElse(null);
    }

    private Optional<String> resolveCountry(String phone) {
        try {
            String normalized = phone.startsWith("+") ? phone : "+" + phone;
            PhoneNumber parsed = phoneNumberUtil.parse(normalized, null);
            String regionCode = phoneNumberUtil.getRegionCodeForNumber(parsed);

            if (regionCode == null) {
                return Optional.empty();
            }

            Locale region = new Locale.Builder().setRegion(regionCode).build();

            return Optional.of(region.getDisplayCountry(Locale.ENGLISH));
        } catch (NumberParseException e) {

            return Optional.empty();
        }
    }
}
