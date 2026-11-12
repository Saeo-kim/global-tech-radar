package com.globaltechradar.collector;

import com.globaltechradar.config.NewsProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;
import java.io.IOException;
import java.security.KeyStore;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.InflaterInputStream;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.URI;
import java.net.UnknownHostException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * Fetches feeds over HTTP(S) with timeouts, a size cap and a generic User-Agent.
 * Refuses non-HTTP schemes and (unless allowed) private / loopback hosts, because users can
 * register their own feed URLs and the server fetches them.
 */
@Component
public class HttpFeedFetcher implements FeedFetcher {

    private static final Logger log = LoggerFactory.getLogger(HttpFeedFetcher.class);

    private final NewsProperties props;
    private final HttpClient client;

    public HttpFeedFetcher(NewsProperties props) {
        this.props = props;
        HttpClient.Builder builder = HttpClient.newBuilder()
            .connectTimeout(props.requestTimeout())
            .followRedirects(HttpClient.Redirect.NEVER);   // followed manually in fetch()
        SSLContext ssl = sslContext(props.trustStore());
        if (ssl != null) builder.sslContext(ssl);
        this.client = builder.build();
    }

    /**
     * Certificates are always validated. {@code os} (or {@code auto} on Windows) validates against the
     * Windows certificate store instead of the JDK's bundled one, so roots installed by the OS, a company
     * proxy or antivirus HTTPS scanning are trusted exactly as the browser trusts them.
     */
    static SSLContext sslContext(String mode) {
        String m = mode == null ? "auto" : mode.trim().toLowerCase(Locale.ROOT);
        boolean windows = System.getProperty("os.name", "").toLowerCase(Locale.ROOT).contains("win");
        if (m.equals("jdk") || (m.equals("auto") && !windows)) return null;
        try {
            KeyStore ks = KeyStore.getInstance("Windows-ROOT");
            ks.load(null, null);
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            tmf.init(ks);
            SSLContext ctx = SSLContext.getInstance("TLS");
            ctx.init(null, tmf.getTrustManagers(), null);
            log.info("News feed fetcher validates TLS certificates against the Windows certificate store");
            return ctx;
        } catch (Exception e) {
            log.warn("OS certificate store unavailable ({}); using the JDK trust store", e.getMessage());
            return null;
        }
    }

    private static final int MAX_REDIRECTS = 5;

    /**
     * Redirects are followed manually so every hop is re-validated: otherwise a public feed could
     * redirect the server to a private address (SSRF) or silently downgrade HTTPS to HTTP.
     */
    @Override
    public byte[] fetch(String url) throws FeedException {
        URI uri = validate(url, props.allowPrivateHosts());
        for (int hop = 0; hop <= MAX_REDIRECTS; hop++) {
            HttpResponse<InputStream> response = send(uri);
            int status = response.statusCode();
            if (status >= 300 && status < 400 && status != 304) {
                closeQuietly(response.body());
                String location = response.headers().firstValue("Location")
                    .orElseThrow(() -> new FeedException("Redirect (HTTP " + status + ") without a Location header"));
                URI next = uri.resolve(location);
                if ("https".equalsIgnoreCase(uri.getScheme()) && "http".equalsIgnoreCase(next.getScheme())) {
                    throw new FeedException("Redirect to " + next + " refused: HTTPS to HTTP downgrade. Register an HTTPS URL instead.");
                }
                uri = validate(next.toString(), props.allowPrivateHosts());
                continue;
            }
            return readBody(uri, response);
        }
        throw new FeedException("Too many redirects (more than " + MAX_REDIRECTS + ")");
    }

    private HttpResponse<InputStream> send(URI uri) throws FeedException {
        HttpRequest request = HttpRequest.newBuilder(uri)
            .timeout(props.requestTimeout())
            .header("User-Agent", props.userAgent())
            .header("Accept", "application/rss+xml, application/atom+xml, application/xml;q=0.9, text/xml;q=0.8, */*;q=0.5")
            // Some CDNs gzip regardless of the request; asking explicitly and decoding below handles both cases.
            .header("Accept-Encoding", "gzip")
            .GET().build();
        try {
            return client.send(request, HttpResponse.BodyHandlers.ofInputStream());
        } catch (IOException e) {
            throw new FeedException("Network error: " + e.getClass().getSimpleName()
                + (e.getMessage() == null ? "" : " - " + e.getMessage()), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new FeedException("Interrupted", e);
        }
    }

    private byte[] readBody(URI uri, HttpResponse<InputStream> response) throws FeedException {
        String encoding = response.headers().firstValue("Content-Encoding").orElse("").trim().toLowerCase(Locale.ROOT);
        try (InputStream raw = response.body();
             InputStream in = encoding.equals("gzip") ? new GZIPInputStream(raw)
                 : encoding.equals("deflate") ? new InflaterInputStream(raw) : raw) {
            {
                int status = response.statusCode();
                if (status >= 400) {
                    throw new FeedException("HTTP " + status + " from " + uri.getHost());
                }
                // Limit applies to the decompressed size, so a small compressed payload cannot expand unbounded.
                byte[] body = in.readNBytes(props.maxFeedBytes() + 1);
                if (body.length > props.maxFeedBytes()) {
                    throw new FeedException("Feed larger than " + props.maxFeedBytes() / 1024 + " KB");
                }
                if (body.length == 0) throw new FeedException("Empty response");
                return body;
            }
        } catch (IOException e) {
            throw new FeedException("Network error while reading: " + e.getClass().getSimpleName()
                + (e.getMessage() == null ? "" : " - " + e.getMessage()), e);
        }
    }

    private static void closeQuietly(InputStream in) {
        try { if (in != null) in.close(); } catch (IOException ignored) { }
    }

    /** Public for reuse by source registration validation. */
    public static URI validate(String url, boolean allowPrivateHosts) throws FeedException {
        URI uri;
        try {
            uri = URI.create(url == null ? "" : url.trim());
        } catch (IllegalArgumentException e) {
            throw new FeedException("Invalid URL");
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme().toLowerCase();
        if (!scheme.equals("http") && !scheme.equals("https")) throw new FeedException("Only http and https feeds are supported");
        if (uri.getHost() == null || uri.getHost().isBlank()) throw new FeedException("URL has no host");
        if (!allowPrivateHosts) {
            try {
                for (InetAddress a : InetAddress.getAllByName(uri.getHost())) {
                    if (a.isLoopbackAddress() || a.isSiteLocalAddress() || a.isLinkLocalAddress() || a.isAnyLocalAddress()) {
                        throw new FeedException("Private or local network addresses are not allowed");
                    }
                }
            } catch (UnknownHostException e) {
                throw new FeedException("Unknown host: " + uri.getHost());
            }
        }
        return uri;
    }
}
