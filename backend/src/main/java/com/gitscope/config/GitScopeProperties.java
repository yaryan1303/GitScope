package com.gitscope.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Externally configurable settings bound from {@code application.properties}
 * under the {@code gitscope.*} prefix.
 *
 * <p>The {@code aliases} map lets operators pin a raw git author name to a
 * canonical display name, e.g.:
 * <pre>
 * gitscope.aliases.sameep-sehgal-1=Sameep Sehgal
 * gitscope.aliases.SameepSehgal=Sameep Sehgal
 * </pre>
 */
@ConfigurationProperties(prefix = "gitscope")
public class GitScopeProperties {

    /** Explicit map of raw git author name -> canonical display name. */
    private Map<String, String> aliases = new LinkedHashMap<>();

    public Map<String, String> getAliases() {
        return aliases;
    }

    public void setAliases(Map<String, String> aliases) {
        this.aliases = aliases;
    }
}
