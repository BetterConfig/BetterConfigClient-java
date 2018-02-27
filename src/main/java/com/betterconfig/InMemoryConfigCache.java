package com.betterconfig;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.util.concurrent.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Optional;
import java.util.concurrent.*;

/**
 * An in-memory cache implementation used to store the fetched configurations.
 */
public class InMemoryConfigCache extends ConfigCache {
    private static final Logger logger = LoggerFactory.getLogger(InMemoryConfigCache.class);
    private ListeningExecutorService executorService;
    private LoadingCache<String, Optional<String>> cache;

    private InMemoryConfigCache(ConfigFetcher configFetcher, Builder builder) {
        super(configFetcher);

        CacheLoader<String, Optional<String>> loader = builder.asyncRefresh
                ? this.createAsyncLoader()
                : this.createSyncLoader();

        this.cache = CacheBuilder.newBuilder().refreshAfterWrite(builder.cacheTimeoutInSeconds, TimeUnit.SECONDS).build(loader);
    }

    @Override
    public String get() {
        return this.cache.getUnchecked("config").orElse(null);
    }

    @Override
    public void invalidateCache() { this.cache.invalidateAll(); }

    @Override
    public void close() throws IOException {
        super.close();
        if (executorService != null) {
            executorService.shutdownNow();
        }
    }

    private CacheLoader<String, Optional<String>> createAsyncLoader() {
        ThreadFactory threadFactory = new ThreadFactoryBuilder().setNameFormat("BetterConfig-refresh-%d").setDaemon(true).build();
        ExecutorService parentExecutor = Executors.newSingleThreadExecutor(threadFactory);
        executorService = MoreExecutors.listeningDecorator(parentExecutor);

        return new CacheLoader<String, Optional<String>>() {
            @Override
            public Optional<String> load(String key) {
                return Optional.ofNullable(InMemoryConfigCache.super.fetcher().getConfigurationJsonString().config());
            }

            @Override
            public ListenableFuture<Optional<String>> reload(String key, Optional<String> oldValue) {
                logger.debug("Async refreshing cached configuration.");
                ListenableFuture<Optional<String>> listenableFuture = executorService.submit(() -> {
                    FetchResponse response = InMemoryConfigCache.super.fetcher().getConfigurationJsonString();
                    return response.isFetched() ? Optional.of(response.config()) : oldValue;
                });
                return listenableFuture;
            }
        };
    }

    private CacheLoader<String, Optional<String>> createSyncLoader() {
        return new CacheLoader<String, Optional<String>>() {
            @Override
            public Optional<String> load(String key) {
                return Optional.ofNullable(InMemoryConfigCache.super.fetcher().getConfigurationJsonString().config());
            }

            @Override
            public ListenableFuture<Optional<String>> reload(String key, Optional<String> oldValue) {
                logger.debug("Sync refreshing cached configuration.");
                FetchResponse response = InMemoryConfigCache.super.fetcher().getConfigurationJsonString();
                return Futures.immediateFuture(response.isFetched() ? Optional.of(response.config()) : oldValue);
            }
        };
    }

    /**
     * Creates a new builder instance.
     *
     * @return the new builder.
     */
    public static Builder Builder() {
        return new Builder();
    }

    /**
     * A builder that helps construct an {@link InMemoryConfigCache} instance.
     */
    public static class Builder {
        private int cacheTimeoutInSeconds;
        private boolean asyncRefresh;

        /**
         * Sets how long the cache will store its value before fetching the
         * latest from the network again.
         *
         * @param cacheTimeoutInSeconds the timeout value in seconds.
         * @return the builder.
         */
        public Builder cacheTimeoutInSeconds(int cacheTimeoutInSeconds) {
            this.cacheTimeoutInSeconds = cacheTimeoutInSeconds;
            return this;
        }

        /**
         * Sets whether the cache should refresh itself asynchronously or synchronously.
         * <p>If it's set to {@code true} the cache will not block the caller thread when the
         * cache timeout is reached. Until the refresh is finished it returns with the previous
         * stored value.</p>
         * <p>If it's set to {@code false} the cache will block the caller thread until the expired
         * value is being refreshed with the latest configuration.</p>
         *
         * @param asyncRefresh the refresh behavior.
         * @return the builder.
         */
        public Builder asyncRefresh(boolean asyncRefresh) {
            this.asyncRefresh = asyncRefresh;
            return this;
        }

        /**
         * Builds the configured {@link InMemoryConfigCache} instance.
         *
         * @param configFetcher the config fetcher instance.
         * @return the configured {@link InMemoryConfigCache} instance.
         */
        public InMemoryConfigCache build(ConfigFetcher configFetcher) {
            return new InMemoryConfigCache(configFetcher,this);
        }
    }
}
