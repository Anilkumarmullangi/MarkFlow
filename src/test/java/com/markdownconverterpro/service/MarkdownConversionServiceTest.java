package com.markdownconverterpro.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

import com.markdownconverterpro.sanitizer.HtmlSanitizer;

class MarkdownConversionServiceTest {

    private static final Pattern EVENT_HANDLER_ATTRIBUTE = Pattern.compile("(?i)<[^>]*\\son\\w+\\s*=");

    private final MarkdownConversionService service = new MarkdownConversionService(new HtmlSanitizer());

    @ParameterizedTest
    @ValueSource(ints = {1, 2, 3, 4, 5, 6})
    void convertsHeadingsOneThroughSix(int level) {
        String html = service.convert("#".repeat(level) + " Title");

        assertEquals("<h" + level + ">Title</h" + level + ">", html);
    }

    @Test
    void convertsParagraphsAndLineBreaks() {
        assertEquals("<p>First</p>\n<p>Second</p>", service.convert("First\n\nSecond"));
        assertTrue(service.convert("one  \ntwo").contains("<br />"));
        assertTrue(service.convert("one\ntwo").contains("<br />"));
    }

    @Test
    void convertsBoldItalicStrikethroughAndInlineCode() {
        assertEquals("<p><strong>bold</strong> and <em>italic</em></p>",
                service.convert("**bold** and *italic*"));
        assertEquals("<p><del>gone</del></p>", service.convert("~~gone~~"));
        assertEquals("<p>use <code>mvn test</code></p>", service.convert("use `mvn test`"));
    }

    @Test
    void convertsUnorderedOrderedAndNestedLists() {
        String unordered = service.convert("- apple\n- banana");
        assertTrue(unordered.contains("<ul>"));
        assertTrue(unordered.contains("<li>apple</li>"));

        String ordered = service.convert("1. first\n2. second");
        assertTrue(ordered.contains("<ol>"));
        assertTrue(ordered.contains("<li>second</li>"));

        String nested = service.convert("- parent\n  - child");
        assertTrue(Pattern.compile("<ul>.*<ul>.*<li>child</li>", Pattern.DOTALL).matcher(nested).find());
    }

    @Test
    void convertsBlockquotes() {
        String html = service.convert("> quoted text");

        assertTrue(html.contains("<blockquote>"));
        assertTrue(html.contains("quoted text"));
    }

    @Test
    void convertsFencedCodeBlocksAndEscapesTheirContent() {
        String html = service.convert("```java\nif (a < b) { run(); }\n```");

        assertTrue(html.contains("<pre><code class=\"language-java\">"));
        assertTrue(html.contains("&lt;"));
        assertFalse(html.contains("a < b"));
    }

    @Test
    void convertsHorizontalRules() {
        assertTrue(service.convert("above\n\n---\n\nbelow").contains("<hr />"));
    }

    @Test
    void convertsLinksWithSafeRelAttributes() {
        String html = service.convert("[Example](https://example.com \"Home\")");

        assertTrue(html.contains("href=\"https://example.com\""));
        assertTrue(html.contains("title=\"Home\""));
        assertTrue(html.contains("rel=\"noopener noreferrer nofollow\""));
        assertTrue(html.contains(">Example</a>"));
    }

    @Test
    void keepsRelativeAndMailtoLinks() {
        String html = service.convert("[a](/docs) [b](#top) [c](mailto:dev@example.com)");

        assertTrue(html.contains("href=\"/docs\""));
        assertTrue(html.contains("href=\"#top\""));
        assertTrue(html.contains("href=\"mailto:"));
    }

    @Test
    void convertsImages() {
        String html = service.convert("![Logo](https://example.com/logo.png \"Brand\")");

        assertTrue(html.contains("<img src=\"https://example.com/logo.png\""));
        assertTrue(html.contains("alt=\"Logo\""));
        assertTrue(html.contains("title=\"Brand\""));
    }

    @Test
    void convertsTablesWithAlignment() {
        String html = service.convert("| Name | Score |\n|:-----|------:|\n| Ada | 10 |");

        assertTrue(html.contains("<table>"));
        assertTrue(html.contains("<th align=\"left\">Name</th>"));
        assertTrue(html.contains("<th align=\"right\">Score</th>"));
        assertTrue(html.contains("<td align=\"left\">Ada</td>"));
    }

    @Test
    void convertsTaskLists() {
        String html = service.convert("- [x] done\n- [ ] todo");

        assertEquals(2, countOccurrences(html, "type=\"checkbox\""));
        assertEquals(1, countOccurrences(html, "checked"));
        assertTrue(html.contains("done"));
        assertTrue(html.contains("todo"));
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {"   ", "\n\n", "\t"})
    void returnsEmptyHtmlForEmptyOrBlankInput(String markdown) {
        assertEquals("", service.convert(markdown));
    }

    @Test
    void escapesRawScriptTags() {
        String html = service.convert("hello <script>alert('xss')</script>");

        assertFalse(html.contains("<script"));
        assertTrue(html.contains("&lt;script&gt;"));
    }

    @Test
    void doesNotRenderRawHtmlElementsOrEventHandlers() {
        String[] attacks = {
                "<img src=x onerror=alert(1)>",
                "<a href=\"https://example.com\" onclick=\"alert(1)\">click</a>",
                "<div onmouseover=\"alert(1)\">hover</div>",
                "<iframe src=\"https://evil.example\"></iframe>",
                "<svg onload=alert(1)>",
                "<body onload=alert(1)>"
        };
        for (String attack : attacks) {
            String html = service.convert(attack);
            assertFalse(EVENT_HANDLER_ATTRIBUTE.matcher(html).find(), "event handler survived for: " + attack);
            assertFalse(html.contains("<img"), "raw img survived for: " + attack);
            assertFalse(html.contains("<iframe"), "iframe survived for: " + attack);
            assertFalse(html.contains("<svg"), "svg survived for: " + attack);
        }
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "[x](javascript:alert(1))",
            "[x](JaVaScRiPt:alert(1))",
            "[x](&#106;avascript:alert(1))",
            "[x](vbscript:msgbox(1))",
            "[x](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==)"
    })
    void dropsDangerousLinkProtocols(String markdown) {
        String html = service.convert(markdown);

        assertFalse(html.contains("href"), html);
        assertFalse(html.toLowerCase().contains("javascript:"), html);
        assertFalse(html.toLowerCase().contains("data:text"), html);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "![x](javascript:alert(1))",
            "![x](data:text/html;base64,PHNjcmlwdD5hbGVydCgxKTwvc2NyaXB0Pg==)"
    })
    void dropsDangerousImageSources(String markdown) {
        String html = service.convert(markdown);

        assertFalse(html.contains("src="), html);
    }

    @Test
    void cannotInjectAttributesThroughLinkTitles() {
        String html = service.convert("[x](https://example.com \"\\\" onmouseover=\\\"alert(1)\")");

        assertFalse(EVENT_HANDLER_ATTRIBUTE.matcher(html).find(), html);
    }

    @Test
    void handlesUnicodeContent() {
        String html = service.convert("வணக்கம் 你好 🚀");

        assertTrue(html.startsWith("<p>வணக்கம் 你好 "));
        assertTrue(html.contains("🚀") || html.contains("&#x1f680;"), html);
    }

    private static int countOccurrences(String text, String needle) {
        int count = 0;
        int index = text.indexOf(needle);
        while (index >= 0) {
            count++;
            index = text.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
