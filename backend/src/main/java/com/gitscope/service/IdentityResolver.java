package com.gitscope.service;

import com.gitscope.config.GitScopeProperties;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Merges duplicate git author identities into a single canonical contributor.
 *
 * <p>Resolution has two layers:
 * <ol>
 *   <li><b>Explicit alias map</b> — an operator-configured {@code raw name ->
 *       canonical name} table (see {@link GitScopeProperties}). Looked up
 *       case-insensitively.</li>
 *   <li><b>Normalization fallback</b> — lowercase the name, strip the token
 *       "veersa", then remove all spaces, hyphens and digits. Names that
 *       collapse to the same normalized form are treated as one person, so
 *       "SameepSehgal", "Sameep Sehgal" and "sameep-sehgal-1" all merge.</li>
 * </ol>
 *
 * <p>The {@link #key(String)} method produces the order-independent grouping
 * key used while walking history; {@link #displayName(Set)} chooses the most
 * human-readable canonical name once all raw aliases for a group are known.
 */
@Service
public class IdentityResolver {

    /** Explicit aliases keyed by lower-cased raw name -> canonical display name. */
    private final Map<String, String> explicitAliases = new HashMap<>();

    public IdentityResolver(GitScopeProperties properties) {
        if (properties.getAliases() != null) {
            properties.getAliases().forEach((raw, canonical) ->
                    explicitAliases.put(raw.toLowerCase(Locale.ROOT), canonical));
        }
    }

    /**
     * The grouping key for a raw git author name. Applies the explicit alias
     * map first (so the configured canonical name drives the key), then the
     * normalization fallback.
     */
    public String key(String rawName) {
        String basis = canonicalFor(rawName);
        if (basis == null) {
            basis = rawName == null ? "" : rawName;
        }
        return normalize(basis);
    }

    /**
     * Returns the explicit canonical name configured for a raw name, or
     * {@code null} if none is configured.
     */
    public String canonicalFor(String rawName) {
        if (rawName == null) {
            return null;
        }
        return explicitAliases.get(rawName.toLowerCase(Locale.ROOT));
    }

    /**
     * Chooses the best canonical display name for a group, given every raw
     * alias that merged into it. An explicit mapping wins; otherwise the most
     * readable raw name is selected (prefers a name containing whitespace,
     * then the longest, then lexicographic order for determinism).
     */
    public String displayName(Set<String> rawAliases) {
        // 1. Honour an explicit canonical mapping if any alias has one.
        for (String raw : rawAliases) {
            String explicit = canonicalFor(raw);
            if (explicit != null) {
                return explicit;
            }
        }
        // 2. Fall back to the most readable raw alias.
        String best = null;
        for (String raw : rawAliases) {
            if (best == null || isMoreReadable(raw, best)) {
                best = raw;
            }
        }
        return best == null ? "(unknown)" : best;
    }

    private boolean isMoreReadable(String candidate, String current) {
        boolean candHasSpace = candidate.contains(" ");
        boolean curHasSpace = current.contains(" ");
        if (candHasSpace != curHasSpace) {
            return candHasSpace;
        }
        if (candidate.length() != current.length()) {
            return candidate.length() > current.length();
        }
        return candidate.compareTo(current) < 0;
    }

    /**
     * Normalization fallback: lowercase, drop the literal token "veersa", then
     * strip spaces, hyphens and digits. The remaining letters form the key.
     */
    public String normalize(String name) {
        if (name == null) {
            return "";
        }
        String n = name.toLowerCase(Locale.ROOT);
        n = n.replace("veersa", "");
        // Remove spaces, hyphens and digits.
        n = n.replaceAll("[\\s\\-0-9]", "");
        return n;
    }

    /** Exposes the configured explicit alias map (read-only view). */
    public Map<String, String> explicitAliases() {
        return new LinkedHashMap<>(explicitAliases);
    }
}
