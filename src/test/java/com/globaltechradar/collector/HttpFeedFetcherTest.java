package com.globaltechradar.collector;

import com.globaltechradar.config.NewsProperties;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.zip.GZIPOutputStream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Exercises the real HTTP fetcher against a local JDK server (no internet). */
class HttpFeedFetcherTest {

    private static final String RSS = "<rss><channel><item><title>Hello</title></item></channel></rss>";
    private HttpServer server;
    private String base;

    @BeforeEach
    void start() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        // Gzip-encoded even though the client may not have asked (seen on a real CDN).
        server.createContext("/gzip", ex -> {
            byte[] body = gzip(RSS.getBytes(StandardCharsets.UTF_8));
            ex.getResponseHeaders().add("Content-Encoding", "gzip");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream o = ex.getResponseBody()) { o.write(body); }
        });
        server.createContext("/plain", ex -> {
            byte[] body = RSS.getBytes(StandardCharsets.UTF_8);
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream o = ex.getResponseBody()) { o.write(body); }
        });
        server.createContext("/moved", ex -> redirect(ex, "/plain"));
        server.createContext("/loop", ex -> redirect(ex, "/loop"));
        server.createContext("/to-file", ex -> redirect(ex, "file:///etc/passwd"));
        server.createContext("/big", ex -> {
            byte[] body = gzip(new byte[200_000]);  // tiny compressed, large decompressed
            ex.getResponseHeaders().add("Content-Encoding", "gzip");
            ex.sendResponseHeaders(200, body.length);
            try (OutputStream o = ex.getResponseBody()) { o.write(body); }
        });
        server.start();
        base = "http://127.0.0.1:" + server.getAddress().getPort();
    }

    @AfterEach
    void stop() { server.stop(0); }

    private static void redirect(com.sun.net.httpserver.HttpExchange ex, String location) throws IOException {
        ex.getResponseHeaders().add("Location", location);
        ex.sendResponseHeaders(302, -1);
        ex.close();
    }

    private static HttpFeedFetcher fetcher(boolean allowPrivate, int maxBytes) {
        return new HttpFeedFetcher(new NewsProperties(true, null, null, Duration.ofSeconds(5), 25, maxBytes, 30,
            null, allowPrivate, "jdk", List.of()));
    }

    private static byte[] gzip(byte[] data) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        try (GZIPOutputStream g = new GZIPOutputStream(bos)) { g.write(data); }
        return bos.toByteArray();
    }

    @Test
    void decodesGzipAndPlainBodies() throws Exception {
        HttpFeedFetcher f = fetcher(true, 1_000_000);
        assertThat(new String(f.fetch(base + "/gzip"), StandardCharsets.UTF_8)).isEqualTo(RSS);
        assertThat(new String(f.fetch(base + "/plain"), StandardCharsets.UTF_8)).isEqualTo(RSS);
    }

    @Test
    void followsRedirectsButRevalidatesEveryHop() throws Exception {
        HttpFeedFetcher f = fetcher(true, 1_000_000);
        assertThat(new String(f.fetch(base + "/moved"), StandardCharsets.UTF_8)).isEqualTo(RSS);
        assertThatThrownBy(() -> f.fetch(base + "/loop")).isInstanceOf(FeedException.class).hasMessageContaining("Too many redirects");
        assertThatThrownBy(() -> f.fetch(base + "/to-file")).isInstanceOf(FeedException.class).hasMessageContaining("Only http and https");
    }

    @Test
    void reportsHttpErrors() {
        assertThatThrownBy(() -> fetcher(true, 1_000_000).fetch(base + "/missing"))
            .isInstanceOf(FeedException.class).hasMessageContaining("HTTP 404");
    }

    @Test
    void sizeLimitAppliesAfterDecompression() {
        assertThatThrownBy(() -> fetcher(true, 50_000).fetch(base + "/big"))
            .isInstanceOf(FeedException.class).hasMessageContaining("larger than");
    }

    @Test
    void refusesLoopbackUnlessExplicitlyAllowed() {
        assertThatThrownBy(() -> fetcher(false, 1_000_000).fetch(base + "/plain"))
            .isInstanceOf(FeedException.class).hasMessageContaining("Private");
    }
}
