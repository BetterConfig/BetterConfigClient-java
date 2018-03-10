package com.betterconfig;

import java.util.concurrent.CompletableFuture;

public class FetchAlwaysPolicy extends RefreshPolicy {
       /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache         the internal cache instance.
     */
    public FetchAlwaysPolicy(ConfigFetcher configFetcher, ConfigCache cache) {
        super(configFetcher, cache);
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        return super.fetcher().getConfigurationJsonStringAsync()
                .thenApply(response -> {
                    String cached = super.cache().get();
                    if (response.isFetched() && !response.config().equals(cached)) {
                        super.cache().set(response.config());
                    }

                    return response.isFetched() ? response.config() : cached;
                });
    }
}
