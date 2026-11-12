package com.globaltechradar.collector;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FeedParserTest {

    private final FeedParser parser = new FeedParser();

    private static byte[] b(String s) { return s.getBytes(StandardCharsets.UTF_8); }

    @Test
    void parsesRss2WithHtmlSummaryAndRfc822Date() throws Exception {
        List<FeedEntry> e = parser.parse(b("""
            <?xml version="1.0"?>
            <rss version="2.0" xmlns:content="http://purl.org/rss/1.0/modules/content/"><channel><title>T</title>
              <item>
                <title>New chip &amp; packaging update</title>
                <link>https://example.com/a?utm_source=x</link>
                <description><![CDATA[<p>Hello <b>world</b>&nbsp;&#8212; details</p>]]></description>
                <pubDate>Wed, 11 Nov 2026 18:00:00 +0000</pubDate>
              </item>
              <item><title>  </title><link>https://example.com/empty</link></item>
            </channel></rss>"""));
        assertThat(e).hasSize(1);
        assertThat(e.get(0).title()).isEqualTo("New chip & packaging update");
        assertThat(e.get(0).link()).isEqualTo("https://example.com/a?utm_source=x");
        assertThat(e.get(0).summary()).isEqualTo("Hello world — details");
        assertThat(e.get(0).publishedAt()).isEqualTo(Instant.parse("2026-11-11T18:00:00Z"));
    }

    @Test
    void parsesTwoDigitYearsAndTextZones() {
        assertThat(FeedParser.parseDate("Tue, 10 Nov 26 12:00:00 +0000")).isEqualTo(Instant.parse("2026-11-10T12:00:00Z"));
        assertThat(FeedParser.parseDate("Tue, 10 Nov 2026 12:00:00 GMT")).isEqualTo(Instant.parse("2026-11-10T12:00:00Z"));
        assertThat(FeedParser.parseDate("2026-11-02T05:51:28.036-05:00")).isEqualTo(Instant.parse("2026-11-02T10:51:28.036Z"));
        assertThat(FeedParser.parseDate("not a date")).isNull();
        assertThat(FeedParser.parseDate(null)).isNull();
    }

    @Test
    void parsesAtomPreferringAlternateLink() throws Exception {
        List<FeedEntry> e = parser.parse(b("""
            <feed xmlns="http://www.w3.org/2005/Atom"><title>Blog</title>
              <entry>
                <title type="html">Security update</title>
                <link rel="replies" href="https://example.com/comments"/>
                <link rel="alternate" href="https://example.com/post"/>
                <published>2026-11-01T10:00:00Z</published>
                <content type="html">&lt;p&gt;Patch now&lt;/p&gt;</content>
              </entry>
            </feed>"""));
        assertThat(e).singleElement().satisfies(x -> {
            assertThat(x.link()).isEqualTo("https://example.com/post");
            assertThat(x.summary()).isEqualTo("Patch now");
            assertThat(x.publishedAt()).isEqualTo(Instant.parse("2026-11-01T10:00:00Z"));
        });
    }

    @Test
    void parsesRss1Rdf() throws Exception {
        List<FeedEntry> e = parser.parse(b("""
            <rdf:RDF xmlns:rdf="http://www.w3.org/1999/02/22-rdf-syntax-ns#" xmlns="http://purl.org/rss/1.0/" xmlns:dc="http://purl.org/dc/elements/1.1/">
              <channel><title>R</title></channel>
              <item><title>Paper</title><link>https://example.org/p</link><dc:date>2026-10-01</dc:date></item>
            </rdf:RDF>"""));
        assertThat(e).singleElement().satisfies(x -> assertThat(x.publishedAt()).isEqualTo(Instant.parse("2026-10-01T00:00:00Z")));
    }

    @Test
    void rejectsInvalidXmlNonFeedsAndDoctypes() {
        assertThatThrownBy(() -> parser.parse(b("<rss><channel><item>"))).isInstanceOf(FeedException.class).hasMessageContaining("Invalid XML");
        assertThatThrownBy(() -> parser.parse(b("<html><body>Not a feed</body></html>"))).isInstanceOf(FeedException.class).hasMessageContaining("Not an RSS");
        assertThatThrownBy(() -> parser.parse(b("""
            <?xml version="1.0"?><!DOCTYPE rss [<!ENTITY xxe SYSTEM "file:///etc/passwd">]>
            <rss><channel><item><title>&xxe;</title></item></channel></rss>"""))).isInstanceOf(FeedException.class);
    }
}
