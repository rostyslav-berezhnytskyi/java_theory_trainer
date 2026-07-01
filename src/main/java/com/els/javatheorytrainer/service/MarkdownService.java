package com.els.javatheorytrainer.service;

import org.commonmark.node.Node;
import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.jsoup.Jsoup;
import org.jsoup.safety.Safelist;
import org.springframework.stereotype.Service;

/**
 * Converts Markdown text to safe HTML for Thymeleaf views.
 */
@Service
public class MarkdownService {

    private final Parser parser = Parser.builder().build();

    private final HtmlRenderer renderer = HtmlRenderer.builder()
            .escapeHtml(true)
            .build();

    public String toHtml(String markdown) {
        if (markdown == null || markdown.isBlank()) {
            return "";
        }

        Node document = parser.parse(markdown);
        String html = renderer.render(document);

        return Jsoup.clean(html, Safelist.relaxed()
                .addTags("pre", "code", "hr")
                .addAttributes("a", "target", "rel"));
    }
}
