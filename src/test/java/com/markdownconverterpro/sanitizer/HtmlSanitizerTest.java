package com.markdownconverterpro.sanitizer;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class HtmlSanitizerTest {

    private final HtmlSanitizer sanitizer = new HtmlSanitizer();

    @ParameterizedTest
    @NullAndEmptySource
    void returnsEmptyStringForNullOrEmptyInput(String html) {
        assertEquals("", sanitizer.sanitize(html));
    }

    @Test
    void removesScriptElementsAndTheirContent() {
        String html = sanitizer.sanitize("<p>safe</p><script>alert(1)</script>");

        assertEquals("<p>safe</p>", html);
    }

    @Test
    void removesObfuscatedScriptElements() {
        String html = sanitizer.sanitize("<scr<script>ipt>alert(1)</scr</script>ipt>");

        assertFalse(html.contains("<script"));
    }

    @Test
    void removesEventHandlerAttributesButKeepsAllowedOnes() {
        String html = sanitizer.sanitize("<a href=\"https://example.com\" onclick=\"alert(1)\">go</a>");

        assertTrue(html.contains("href=\"https://example.com\""));
        assertFalse(html.contains("onclick"));
    }

    @Test
    void removesDisallowedElements() {
        String[] elements = {
                "<iframe src=\"https://example.com\"></iframe>",
                "<object data=\"x\"></object>",
                "<embed src=\"x\">",
                "<form action=\"/x\"><button>go</button></form>",
                "<style>body{display:none}</style>",
                "<link rel=\"stylesheet\" href=\"https://example.com/x.css\">",
                "<meta http-equiv=\"refresh\" content=\"0;url=https://evil.example\">",
                "<svg onload=\"alert(1)\"></svg>",
                "<math><mi>x</mi></math>"
        };
        for (String element : elements) {
            String html = sanitizer.sanitize(element);
            assertFalse(html.contains("<iframe"), element);
            assertFalse(html.contains("<object"), element);
            assertFalse(html.contains("<embed"), element);
            assertFalse(html.contains("<form"), element);
            assertFalse(html.contains("<style"), element);
            assertFalse(html.contains("<link"), element);
            assertFalse(html.contains("<meta"), element);
            assertFalse(html.contains("<svg"), element);
            assertFalse(html.contains("<math"), element);
            assertFalse(html.contains("display:none"), element);
        }
    }

    @Test
    void removesStyleAttributes() {
        String html = sanitizer.sanitize("<p style=\"position:fixed;top:0\">text</p>");

        assertEquals("<p>text</p>", html);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "javascript:alert(1)",
            "JAVASCRIPT:alert(1)",
            " javascript:alert(1)",
            "vbscript:msgbox(1)",
            "data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==",
            "file:///etc/passwd"
    })
    void removesDangerousUrlProtocolsFromLinksAndImages(String url) {
        String link = sanitizer.sanitize("<a href=\"" + url + "\">x</a>");
        String image = sanitizer.sanitize("<img src=\"" + url + "\" alt=\"x\">");

        assertFalse(link.contains("href"), link);
        assertFalse(image.contains("src="), image);
    }

    @ParameterizedTest
    @ValueSource(strings = {"https://example.com/a", "http://example.com/a", "mailto:dev@example.com"})
    void keepsStandardUrlProtocolsOnLinks(String url) {
        String html = sanitizer.sanitize("<a href=\"" + url + "\">x</a>");

        assertTrue(html.contains("href="), html);
    }

    @Test
    void addsSafeRelAttributesToEveryLink() {
        String html = sanitizer.sanitize("<a href=\"https://example.com\" rel=\"opener\">x</a>");

        assertTrue(html.contains("noopener"));
        assertTrue(html.contains("noreferrer"));
        assertTrue(html.contains("nofollow"));
        assertFalse(html.contains("rel=\"opener\""));
    }

    @Test
    void restrictsCodeClassesToLanguageNames() {
        String allowed = sanitizer.sanitize("<pre><code class=\"language-java\">x</code></pre>");
        String blocked = sanitizer.sanitize("<pre><code class=\"admin-panel\">x</code></pre>");

        assertTrue(allowed.contains("class=\"language-java\""));
        assertFalse(blocked.contains("class="));
    }

    @Test
    void allowsOnlyDisabledCheckboxInputs() {
        String checkbox = sanitizer.sanitize("<input type=\"checkbox\" checked disabled>");
        String text = sanitizer.sanitize("<input type=\"text\" value=\"x\" onfocus=\"alert(1)\">");

        assertTrue(checkbox.contains("type=\"checkbox\""));
        assertTrue(checkbox.contains("checked"));
        assertFalse(text.contains("type=\"text\""));
        assertFalse(text.contains("onfocus"));
        assertFalse(text.contains("value"));
    }

    @Test
    void restrictsTableAlignmentValues() {
        String allowed = sanitizer.sanitize("<table><tr><td align=\"center\">x</td></tr></table>");
        String blocked = sanitizer.sanitize("<table><tr><td align=\"javascript:alert(1)\">x</td></tr></table>");

        assertTrue(allowed.contains("align=\"center\""));
        assertFalse(blocked.contains("align"));
    }

    @Test
    void keepsAllowedFormattingElements() {
        String input = "<h2>T</h2><p><strong>b</strong><em>i</em><del>d</del><code>c</code></p>"
                + "<blockquote>q</blockquote><ul><li>a</li></ul><hr>";
        String html = sanitizer.sanitize(input);

        assertTrue(html.contains("<h2>T</h2>"));
        assertTrue(html.contains("<strong>b</strong>"));
        assertTrue(html.contains("<blockquote>q</blockquote>"));
        assertTrue(html.contains("<li>a</li>"));
        assertTrue(html.contains("<hr />"));
    }
}
