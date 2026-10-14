package com.markdownconverterpro.sanitizer;

import java.util.regex.Pattern;

import org.owasp.html.HtmlPolicyBuilder;
import org.owasp.html.PolicyFactory;

public class HtmlSanitizer {

    private static final Pattern CODE_LANGUAGE_CLASS = Pattern.compile("language-[A-Za-z0-9_+#.-]{1,30}");
    private static final Pattern TABLE_ALIGNMENT = Pattern.compile("left|right|center");
    private static final Pattern ORDERED_LIST_START = Pattern.compile("[0-9]{1,9}");
    private static final Pattern CHECKBOX_TYPE = Pattern.compile("checkbox");

    private final PolicyFactory policy;

    public HtmlSanitizer() {
        this.policy = buildPolicy();
    }

    public String sanitize(String html) {
        if (html == null || html.isEmpty()) {
            return "";
        }
        return policy.sanitize(html);
    }

    private static PolicyFactory buildPolicy() {
        return new HtmlPolicyBuilder()
                .allowElements(
                        "h1", "h2", "h3", "h4", "h5", "h6",
                        "p", "br", "hr",
                        "strong", "em", "del", "s", "code", "pre", "blockquote",
                        "ul", "ol", "li",
                        "a", "img",
                        "table", "thead", "tbody", "tr", "th", "td",
                        "input")
                .allowStandardUrlProtocols()
                .allowAttributes("href", "title").onElements("a")
                .allowAttributes("src", "alt", "title").onElements("img")
                .allowAttributes("class").matching(CODE_LANGUAGE_CLASS).onElements("code")
                .allowAttributes("start").matching(ORDERED_LIST_START).onElements("ol")
                .allowAttributes("align").matching(TABLE_ALIGNMENT).onElements("th", "td")
                .allowAttributes("type").matching(CHECKBOX_TYPE).onElements("input")
                .allowAttributes("checked", "disabled").onElements("input")
                .requireRelsOnLinks("noopener", "noreferrer", "nofollow")
                .toFactory();
    }
}
