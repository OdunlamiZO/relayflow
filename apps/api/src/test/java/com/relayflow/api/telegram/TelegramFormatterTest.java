package com.relayflow.api.telegram;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class TelegramFormatterTest {

    @Test
    void convertsEachStyle() {
        assertThat(TelegramFormatter.toHtml("*bold* _italic_ ~gone~ `code`"))
                .isEqualTo("<b>bold</b> <i>italic</i> <s>gone</s> <code>code</code>");
    }

    @Test
    void convertsACodeBlockWithoutFormattingInside() {
        assertThat(TelegramFormatter.toHtml("```\nlet *a* = 1;\n```"))
                .isEqualTo("<pre>\nlet *a* = 1;\n</pre>");
    }

    @Test
    void escapesHtml() {
        assertThat(TelegramFormatter.toHtml("Total < 5 & *x > 2*"))
                .isEqualTo("Total &lt; 5 &amp; <b>x &gt; 2</b>");
    }

    @Test
    void nestsStyles() {
        assertThat(TelegramFormatter.toHtml("*bold _and italic_*"))
                .isEqualTo("<b>bold <i>and italic</i></b>");
    }

    @Test
    void ignoresMarkersInsideWords() {
        assertThat(TelegramFormatter.toHtml("snake_case_name and 2*3*4"))
                .isEqualTo("snake_case_name and 2*3*4");
    }

    @Test
    void ignoresMarkersNextToSpaces() {
        assertThat(TelegramFormatter.toHtml("a * b * c")).isEqualTo("a * b * c");
    }

    @Test
    void neverProducesOverlappingTags() {
        assertThat(TelegramFormatter.toHtml("*a _b* c_")).isEqualTo("<b>a _b</b> c_");
    }

    @Test
    void turnsDashAndStarLinesIntoBullets() {
        assertThat(TelegramFormatter.toHtml("Menu:\n- *Rice*\n* Beans\n  - extra"))
                .isEqualTo("Menu:\n• <b>Rice</b>\n• Beans\n  • extra");
    }

    @Test
    void leavesNonBulletDashesAlone() {
        assertThat(TelegramFormatter.toHtml("a - b\n-5 degrees\n*bold* start"))
                .isEqualTo("a - b\n-5 degrees\n<b>bold</b> start");
    }

    @Test
    void doesNotSpanLines() {
        assertThat(TelegramFormatter.toHtml("*one\ntwo*")).isEqualTo("*one\ntwo*");
    }
}
