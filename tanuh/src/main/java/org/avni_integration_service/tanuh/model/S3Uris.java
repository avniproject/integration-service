package org.avni_integration_service.tanuh.model;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

// Tanuh's intake API wants each photo as s3://<bucket>/<key>. Avni stores a photo as an https address in any of the
// shapes S3 uses, so this turns one into the other.
public final class S3Uris {
    // s3.amazonaws.com, s3.<region>.amazonaws.com or s3-<region>.amazonaws.com, optionally behind "<bucket>.".
    private static final Pattern S3_HOST = Pattern.compile("^(?:(.+)\\.)?s3(?:[.-][a-z0-9-]+)?\\.amazonaws\\.com$");

    private S3Uris() {
    }

    // The S3 location of one photo address, or empty when the address is not an S3 object's. The query string (a
    // signature, say) is dropped and the key is decoded.
    public static Optional<String> one(String photoUrl) {
        if (photoUrl == null || photoUrl.isBlank()) return Optional.empty();
        URI uri;
        try {
            uri = new URI(photoUrl.trim());
        } catch (URISyntaxException e) {
            return Optional.empty();
        }
        String scheme = uri.getScheme();
        String host = uri.getHost();
        String path = uri.getPath();
        if (host == null || path == null || scheme == null) return Optional.empty();
        if (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https")) return Optional.empty();
        Matcher matcher = S3_HOST.matcher(host.toLowerCase());
        if (!matcher.matches()) return Optional.empty();

        String bucket = matcher.group(1);
        String key = path.startsWith("/") ? path.substring(1) : path;
        if (bucket == null) {
            int slash = key.indexOf('/');
            if (slash < 0) return Optional.empty();
            bucket = key.substring(0, slash);
            key = key.substring(slash + 1);
        }
        if (bucket.isEmpty() || key.isEmpty()) return Optional.empty();
        return Optional.of("s3://" + bucket + "/" + key);
    }

    // The locations of all the photos, each once and in the order first seen, since the intake API refuses a repeated
    // photo. Empty when any address has no S3 location, as sending a partial set would hide the missing photo.
    public static Optional<List<String>> all(List<String> photoUrls) {
        Set<String> locations = new LinkedHashSet<>();
        for (String url : photoUrls) {
            Optional<String> location = one(url);
            if (location.isEmpty()) return Optional.empty();
            locations.add(location.get());
        }
        return Optional.of(new ArrayList<>(locations));
    }
}
