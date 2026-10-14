package com.markdownconverterpro.service;

import java.util.List;

import org.commonmark.Extension;
import org.commonmark.ext.gfm.strikethrough.StrikethroughExtension;
import org.commonmark.ext.gfm.tables.TablesExtension;
import org.commonmark.ext.task.list.items.TaskListItemsExtension;
import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;

import com.markdownconverterpro.sanitizer.HtmlSanitizer;

public class MarkdownConversionService {

    private final Parser parser;
    private final HtmlRenderer renderer;
    private final HtmlSanitizer sanitizer;

    public MarkdownConversionService(HtmlSanitizer sanitizer) {
        List<Extension> extensions = List.of(
                TablesExtension.create(),
                StrikethroughExtension.create(),
                TaskListItemsExtension.create());
        this.parser = Parser.builder().extensions(extensions).build();
        this.renderer = HtmlRenderer.builder()
                .extensions(extensions)
                .escapeHtml(true)
                .softbreak("<br />\n")
                .build();
        this.sanitizer = sanitizer;
    }

    public String convert(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }
        Node document = parser.parse(markdown);
        String rawHtml = renderer.render(document);
        return sanitizer.sanitize(rawHtml).strip();
    }
}
