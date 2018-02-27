package com.betterconfig;

import java.io.Closeable;
import java.io.IOException;

public abstract class ConfigCache implements Closeable {

    private ConfigFetcher configFetcher;

    protected ConfigFetcher fetcher() {
        return configFetcher;
    }

    /**
     * Constructor used by the inherited classes.
     *
     * @param configFetcher the internal config fetcher instance.
     */
    public ConfigCache(ConfigFetcher configFetcher) {
        this.configFetcher = configFetcher;
    }

    /**
     * Gets the cached configuration.
     *
     * @return the cached configuration.
     */
    public abstract String get();

    /**
     * Invalidates the internal cache, the next read will fetch the configuration from the network.
     */
    public abstract void invalidateCache();

    @Override
    public void close() throws IOException {
        this.configFetcher.close();
    }
}

