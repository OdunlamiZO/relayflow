package com.relayflow.api.contact;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PhoneNumberNormalizerTest {

    private final PhoneNumberNormalizer normalizer = new PhoneNumberNormalizer();

    @Test
    void keepsAnInternationalNumber() {
        assertThat(normalizer.toE164("+234 803 123 4567", null)).contains("+2348031234567");
    }

    @Test
    void treatsBareDigitsAsInternational() {
        assertThat(normalizer.toE164("2348031234567", null)).contains("+2348031234567");
    }

    @Test
    void readsADoubleZeroPrefixAsInternational() {
        assertThat(normalizer.toE164("002348031234567", null)).contains("+2348031234567");
    }

    @Test
    void usesTheDefaultRegionForALocalNumber() {
        assertThat(normalizer.toE164("08031234567", "NG")).contains("+2348031234567");
    }

    @Test
    void rejectsALocalNumberWithoutARegion() {
        assertThat(normalizer.toE164("08031234567", null)).isEmpty();
    }

    @Test
    void rejectsText() {
        assertThat(normalizer.toE164("not a number", "NG")).isEmpty();
    }
}
