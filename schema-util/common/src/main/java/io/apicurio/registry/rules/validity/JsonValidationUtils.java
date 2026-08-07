package io.apicurio.registry.rules.validity;

import com.fasterxml.jackson.databind.JsonNode;
import io.apicurio.registry.rules.violation.RuleViolation;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Set;

/**
 * Shared JSON validation utility methods used by content validators for JSON-based artifact types
 * such as AGENT_CARD and MCP_TOOL.
 */
public final class JsonValidationUtils {

    private JsonValidationUtils() {
        // Utility class
    }

    /**
     * Validates that an optional field, if present, is a string.
     */
    public static void validateOptionalString(JsonNode tree, String fieldName,
            Set<RuleViolation> violations) {
        if (tree.has(fieldName) && !tree.get(fieldName).isTextual()) {
            violations.add(
                    new RuleViolation("'" + fieldName + "' field must be a string", "/" + fieldName));
        }
    }

    /**
     * Validates that a string value is a well-formed HTTP or HTTPS URL.
     *
     * <p>Per RFC 3986 section 3.1, URI schemes are case-insensitive, so the scheme is
     * normalized to lowercase before comparison. Additionally, {@link URI#getHost()} returns
     * {@code null} for IPv6 literal hosts (e.g. {@code http://[::1]:8080/}) on some JDK
     * versions; the authority component is used as a fallback for the presence check.
     */
    public static void validateHttpUrl(String value, String path, Set<RuleViolation> violations) {
        try {
            URI uri = new URI(value);
            String scheme = uri.getScheme();
            // Normalize to lowercase per RFC 3986 §3.1 — schemes are case-insensitive
            if (scheme == null || !scheme.toLowerCase(java.util.Locale.ROOT).matches("https?")) {
                violations.add(new RuleViolation("URL must use http or https scheme", path));
            } else {
                // URI.getHost() returns null for IPv6 literals (e.g. http://[::1]:8080/).
                // Fall back to getAuthority() so valid IPv6 URLs are not incorrectly rejected.
                String host = uri.getHost();
                String authority = uri.getAuthority();
                boolean hasHost = (host != null && !host.trim().isEmpty())
                        || (authority != null && !authority.trim().isEmpty());
                if (!hasHost) {
                    violations.add(new RuleViolation("URL must have a valid host", path));
                }
            }
        } catch (URISyntaxException e) {
            violations.add(new RuleViolation("Invalid URL format: " + e.getMessage(), path));
        }
    }


    /**
     * Validates that an optional field, if present, is a string and a well-formed HTTP(S) URL.
     */
    public static void validateOptionalUrl(JsonNode tree, String fieldName,
            Set<RuleViolation> violations) {
        if (!tree.has(fieldName)) {
            return;
        }
        if (!tree.get(fieldName).isTextual()) {
            violations.add(
                    new RuleViolation("'" + fieldName + "' field must be a string", "/" + fieldName));
            return;
        }
        validateHttpUrl(tree.get(fieldName).asText(), "/" + fieldName, violations);
    }

    /**
     * Validates that an optional field, if present, is an array of strings.
     */
    public static void validateStringArrayField(JsonNode tree, String fieldName,
            Set<RuleViolation> violations) {
        if (!tree.has(fieldName)) {
            return;
        }

        JsonNode array = tree.get(fieldName);
        if (!array.isArray()) {
            violations.add(
                    new RuleViolation("'" + fieldName + "' field must be an array", "/" + fieldName));
            return;
        }

        validateStringArray(array, "/" + fieldName, "item", violations);
    }

    /**
     * Validates that every element in a JSON array is a string.
     */
    public static void validateStringArray(JsonNode array, String basePath, String itemName,
            Set<RuleViolation> violations) {
        int index = 0;
        for (JsonNode item : array) {
            if (!item.isTextual()) {
                violations.add(new RuleViolation("Each " + itemName + " must be a string",
                        basePath + "/" + index));
            }
            index++;
        }
    }
}
