package com.betterconfig;

import java.io.Closeable;
import java.io.IOException;

/**
 * An abstract cache API used to make custom cache implementations for {@link BetterConfigClient}.
 */
public abstract class ConfigCache implements Closeable {

    private ConfigFetcher configFetcher;

    /**
     * Through this getter, child classes can use the fetcher to
     * get the latest configuration over HTTP.
     *
     * @return the config fetcher.
     */
    protected ConfigFetcher fetcher() {
        return configFetcher;
    }

    /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     */
    public ConfigCache(ConfigFetcher configFetcher) {
        this.configFetcher = configFetcher;
    }

    /**
     * Child classes has to implement this method, the {@link BetterConfigClient}
     * uses it to get the actual value from the cache.
     *
     * @return the cached configuration.
     */
    public abstract String get();

    /**
     * Invalidates the underlying cache.
     */
    public abstract void invalidateCache();

    @Override
    public void close() throws IOException {
        this.configFetcher.close();
    }
}

