package com.relayflow.api.contact;

import com.google.i18n.phonenumbers.NumberParseException;
import com.google.i18n.phonenumbers.PhoneNumberUtil;
import com.google.i18n.phonenumbers.PhoneNumberUtil.PhoneNumberFormat;
import com.google.i18n.phonenumbers.Phonenumber.PhoneNumber;
import java.util.Optional;
import org.springframework.stereotype.Component;

@Component
public class PhoneNumberNormalizer {

    private final PhoneNumberUtil phoneNumberUtil = PhoneNumberUtil.getInstance();

    public Optional<String> toE164(String raw, String defaultRegion) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }

        String candidate = raw.trim();

        if (candidate.startsWith("00")) {
            candidate = "+" + candidate.substring(2);
        } else if (!candidate.startsWith("+")
                && !candidate.startsWith("0")
                && candidate.chars().allMatch(Character::isDigit)) {
            candidate = "+" + candidate;
        }

        try {
            PhoneNumber parsed = phoneNumberUtil.parse(candidate, defaultRegion);

            if (!phoneNumberUtil.isPossibleNumber(parsed)) {
                return Optional.empty();
            }

            return Optional.of(phoneNumberUtil.format(parsed, PhoneNumberFormat.E164));
        } catch (NumberParseException e) {
            return Optional.empty();
        }
    }

    public boolean isSupportedRegion(String region) {
        return phoneNumberUtil.getSupportedRegions().contains(region);
    }
}
