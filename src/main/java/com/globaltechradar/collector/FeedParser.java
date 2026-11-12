package com.globaltechradar.collector;

import org.springframework.stereotype.Component;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoField;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parses RSS 2.0 / RSS 1.0 (RDF) and Atom feeds with the JDK XML parser.
 * DOCTYPE declarations and external entities are disabled (XXE protection).
 */
@Component
public class FeedParser {

    private static final int SUMMARY_MAX = 1200;
    private static final Pattern TAGS = Pattern.compile("<[^>]+>");
    private static final Pattern NUMERIC_ENTITY = Pattern.compile("&#(x?[0-9a-fA-F]+);");
    private static final Pattern WS = Pattern.compile("\\s+");

    /**
     * RFC-822 style dates with 2- or 4-digit years and numeric or textual zones.
     * Two-digit-year patterns come first: the 4-digit patterns would otherwise read "26" as year 0026
     * (seen in CISA's feed: "Tue, 10 Nov 26 12:00:00 +0000"). They cannot match a 4-digit year.
     */
    private static final List<DateTimeFormatter> RFC822 = List.of(
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("EEE, d MMM ")
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000).appendPattern(" HH:mm[:ss] Z").toFormatter(Locale.ENGLISH),
        new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern("EEE, d MMM ")
            .appendValueReduced(ChronoField.YEAR, 2, 2, 2000).appendPattern(" HH:mm[:ss] zzz").toFormatter(Locale.ENGLISH),
        DateTimeFormatter.RFC_1123_DATE_TIME,
        rfc("EEE, d MMM yyyy HH:mm[:ss] Z"),
        rfc("EEE, d MMM yyyy HH:mm[:ss] zzz"),
        rfc("d MMM yyyy HH:mm[:ss] Z"));

    private static DateTimeFormatter rfc(String pattern) {
        return new DateTimeFormatterBuilder().parseCaseInsensitive().appendPattern(pattern).toFormatter(Locale.ENGLISH);
    }

    public List<FeedEntry> parse(byte[] xml) throws FeedException {
        Document doc = toDocument(xml);
        Element root = doc.getDocumentElement();
        String rootName = local(root);
        List<FeedEntry> entries = new ArrayList<>();
        if ("feed".equals(rootName)) {
            for (Element e : children(root, "entry")) entries.add(atomEntry(e));
        } else if ("rss".equals(rootName) || "RDF".equals(rootName)) {
            Element container = "rss".equals(rootName) ? first(root, "channel") : root;
            if (container == null) throw new FeedException("RSS feed has no <channel>");
            List<Element> items = children(container, "item");
            if (items.isEmpty() && "RDF".equals(rootName)) items = children(root, "item");
            for (Element i : items) entries.add(rssItem(i));
        } else {
            throw new FeedException("Not an RSS or Atom feed (root element <" + rootName + ">)");
        }
        return entries.stream().filter(e -> e.title() != null && !e.title().isBlank()).toList();
    }

    private Document toDocument(byte[] xml) throws FeedException {
        try {
            DocumentBuilderFactory f = DocumentBuilderFactory.newInstance();
            f.setNamespaceAware(true);
            f.setFeature("http://apache.org/xml/features/disallow-doctype-decl", true);
            f.setFeature("http://xml.org/sax/features/external-general-entities", false);
            f.setFeature("http://xml.org/sax/features/external-parameter-entities", false);
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_DTD, "");
            f.setAttribute(XMLConstants.ACCESS_EXTERNAL_SCHEMA, "");
            f.setXIncludeAware(false);
            f.setExpandEntityReferences(false);
            DocumentBuilder b = f.newDocumentBuilder();
            b.setErrorHandler(null);
            return b.parse(new ByteArrayInputStream(xml));
        } catch (ParserConfigurationException e) {
            throw new FeedException("XML parser unavailable", e);
        } catch (SAXException e) {
            throw new FeedException("Invalid XML: " + e.getMessage(), e);
        } catch (IOException e) {
            throw new FeedException("Could not read feed", e);
        }
    }

    private FeedEntry rssItem(Element item) {
        String title = clean(text(item, "title"));
        String link = text(item, "link");
        if (link == null || link.isBlank()) {
            Element guid = first(item, "guid");
            if (guid != null && !"false".equalsIgnoreCase(guid.getAttribute("isPermaLink"))) link = guid.getTextContent();
        }
        String summary = firstNonBlank(text(item, "description"), text(item, "summary"), text(item, "encoded"));
        Instant date = parseDate(firstNonBlank(text(item, "pubDate"), text(item, "date"), text(item, "published"), text(item, "updated")));
        return new FeedEntry(title, link == null ? null : link.trim(), truncate(clean(summary)), date);
    }

    private FeedEntry atomEntry(Element entry) {
        String title = clean(text(entry, "title"));
        String link = null;
        for (Element l : children(entry, "link")) {
            String rel = l.getAttribute("rel");
            if (rel.isEmpty() || "alternate".equals(rel)) { link = l.getAttribute("href"); break; }
        }
        if (link == null) {
            List<Element> links = children(entry, "link");
            if (!links.isEmpty()) link = links.get(0).getAttribute("href");
        }
        String summary = firstNonBlank(text(entry, "summary"), text(entry, "content"));
        Instant date = parseDate(firstNonBlank(text(entry, "published"), text(entry, "updated")));
        return new FeedEntry(title, link == null ? null : link.trim(), truncate(clean(summary)), date);
    }

    // ------------------------------------------------------------------ helpers

    static Instant parseDate(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String s = WS.matcher(raw.trim()).replaceAll(" ");
        try { return OffsetDateTime.parse(s).toInstant(); } catch (DateTimeParseException ignored) { }
        try { return Instant.parse(s); } catch (DateTimeParseException ignored) { }
        try { return LocalDate.parse(s).atStartOfDay(ZoneOffset.UTC).toInstant(); } catch (DateTimeParseException ignored) { }
        for (DateTimeFormatter f : RFC822) {
            try { return ZonedDateTime.parse(s, f).toInstant(); } catch (DateTimeParseException ignored) { }
        }
        return null;
    }

    /** Strip HTML tags, decode common entities and collapse whitespace. */
    static String clean(String html) {
        if (html == null) return null;
        String s = TAGS.matcher(html).replaceAll(" ");
        s = s.replace("&nbsp;", " ").replace("&amp;", "&").replace("&lt;", "<").replace("&gt;", ">")
            .replace("&quot;", "\"").replace("&#39;", "'").replace("&apos;", "'")
            .replace("&rsquo;", "’").replace("&lsquo;", "‘").replace("&ldquo;", "“")
            .replace("&rdquo;", "”").replace("&mdash;", "—").replace("&ndash;", "–").replace("&hellip;", "…");
        Matcher m = NUMERIC_ENTITY.matcher(s);
        StringBuilder sb = new StringBuilder();
        while (m.find()) {
            String code = m.group(1);
            String rep;
            try {
                int cp = code.startsWith("x") || code.startsWith("X") ? Integer.parseInt(code.substring(1), 16) : Integer.parseInt(code);
                rep = new String(Character.toChars(cp));
            } catch (IllegalArgumentException e) {
                rep = "";
            }
            m.appendReplacement(sb, Matcher.quoteReplacement(rep));
        }
        m.appendTail(sb);
        return WS.matcher(sb.toString()).replaceAll(" ").trim();
    }

    private static String truncate(String s) {
        if (s == null) return null;
        if (s.length() <= SUMMARY_MAX) return s;
        int cut = s.lastIndexOf(' ', SUMMARY_MAX - 1);
        return s.substring(0, cut > SUMMARY_MAX / 2 ? cut : SUMMARY_MAX - 1) + "…";
    }

    private static String firstNonBlank(String... values) {
        for (String v : values) if (v != null && !v.isBlank()) return v;
        return null;
    }

    private static String local(Node n) {
        return n.getLocalName() != null ? n.getLocalName() : n.getNodeName();
    }

    private static List<Element> children(Element parent, String localName) {
        List<Element> out = new ArrayList<>();
        NodeList nodes = parent.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node n = nodes.item(i);
            if (n instanceof Element e && localName.equals(local(e))) out.add(e);
        }
        return out;
    }

    private static Element first(Element parent, String localName) {
        List<Element> c = children(parent, localName);
        return c.isEmpty() ? null : c.get(0);
    }

    private static String text(Element parent, String localName) {
        Element e = first(parent, localName);
        return e == null ? null : e.getTextContent();
    }
}
