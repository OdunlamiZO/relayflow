package com.relayflow.api.telegram;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Converts WhatsApp-style formatting ({@code *bold*}, {@code _italic_}, {@code ~strike~}, {@code
 * `code`}, {@code ```block```}, and {@code - } or {@code * } bullets) to Telegram HTML, escaping
 * everything else. Mirrors {@code message-formatting.ts} in the web app.
 */
public final class TelegramFormatter {

    private static final Pattern CODE_BLOCK = Pattern.compile("```([\\s\\S]+?)```");

    private static final Pattern INLINE_CODE = Pattern.compile("`([^`\\n]+)`");

    private static final Pattern BULLET = Pattern.compile("(?m)^([ \\t]*)[-*] (?=\\S)");

    private static final List<Style> STYLES =
            List.of(style('*', "b"), style('_', "i"), style('~', "s"));

    public static String toHtml(String text) {
        if (text == null) {
            return null;
        }

        StringBuilder html = new StringBuilder();
        Matcher matcher = CODE_BLOCK.matcher(text);
        int position = 0;

        while (matcher.find()) {
            appendInlineCode(html, text.substring(position, matcher.start()));
            html.append("<pre>").append(escape(matcher.group(1))).append("</pre>");
            position = matcher.end();
        }

        appendInlineCode(html, text.substring(position));

        return html.toString();
    }

    private static void appendInlineCode(StringBuilder html, String text) {
        text = BULLET.matcher(text).replaceAll("$1• ");
        Matcher matcher = INLINE_CODE.matcher(text);
        int position = 0;

        while (matcher.find()) {
            appendStyled(html, text.substring(position, matcher.start()));
            html.append("<code>").append(escape(matcher.group(1))).append("</code>");
            position = matcher.end();
        }

        appendStyled(html, text.substring(position));
    }

    private static void appendStyled(StringBuilder html, String text) {
        while (!text.isEmpty()) {
            Style earliest = null;
            Matcher earliestMatch = null;

            for (Style style : STYLES) {
                Matcher matcher = style.pattern().matcher(text);

                if (matcher.find()
                        && (earliestMatch == null || matcher.start() < earliestMatch.start())) {
                    earliest = style;
                    earliestMatch = matcher;
                }
            }

            if (earliestMatch == null) {
                html.append(escape(text));

                return;
            }

            html.append(escape(text.substring(0, earliestMatch.start())));
            html.append('<').append(earliest.tag()).append('>');
            appendStyled(html, earliestMatch.group(1));
            html.append("</").append(earliest.tag()).append('>');
            text = text.substring(earliestMatch.end());
        }
    }

    private static String escape(String text) {
        return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;");
    }

    private static Style style(char marker, String tag) {
        String quoted = Pattern.quote(String.valueOf(marker));

        return new Style(
                tag,
                Pattern.compile(
                        "(?<![\\p{L}\\p{N}])"
                                + quoted
                                + "(\\S(?:[^\\n]*?\\S)?)"
                                + quoted
                                + "(?![\\p{L}\\p{N}])"));
    }

    private record Style(String tag, Pattern pattern) {}

    private TelegramFormatter() {}
}
