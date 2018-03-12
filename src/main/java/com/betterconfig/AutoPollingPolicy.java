package com.betterconfig;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

public class AutoPollingPolicy extends RefreshPolicy {
    private static final Logger logger = LoggerFactory.getLogger(BetterConfigClient.class);
    private final ScheduledExecutorService scheduler;
    private final CompletableFuture<Void> initFuture;
    private final AtomicBoolean initialized;

    /**
     * Constructor used by the child classes.
     *
     * @param configFetcher the internal config fetcher instance.
     * @param cache         the internal cache instance.
     */
    private AutoPollingPolicy(ConfigFetcher configFetcher, ConfigCache cache, Builder builder) {
        super(configFetcher, cache);

        this.initialized = new AtomicBoolean(false);
        this.initFuture = new CompletableFuture<>();
        this.scheduler = Executors.newSingleThreadScheduledExecutor();
        this.scheduler.scheduleAtFixedRate(() -> {
            try {
                FetchResponse response = super.fetcher().getConfigurationJsonStringAsync().get();
                String cached = super.cache().get();
                if (response.isFetched() && !response.config().equals(cached))
                    super.cache().set(response.config());

                if(!response.isFailed() && !initialized.getAndSet(true))
                    initFuture.complete(null);

            } catch (Exception e){
                logger.error("An error occurred during the scheduler poll execution", e);
            }
        }, 0, builder.autoPollRateInSeconds, TimeUnit.SECONDS);
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        if(this.initFuture.isDone())
            return CompletableFuture.completedFuture(super.cache().get());

        return this.initFuture.thenApplyAsync(v -> super.cache().get());
    }

    @Override
    public void close() throws IOException {
        super.close();
        this.scheduler.shutdown();
    }

    /**
     * Creates a new builder instance.
     *
     * @return the new builder.
     */
    public static Builder newBuilder() {
        return new Builder();
    }

    /**
     * A builder that helps construct a {@link AutoPollingPolicy} instance.
     */
    public static class Builder {
        private int autoPollRateInSeconds = 60;

        /**
         * Sets at least how often this policy should fetch the latest configuration and refresh the cache.
         *
         * @param autoPollRateInSeconds the poll rate.
         * @return the builder.
         * @throws IllegalArgumentException when the given value is less than 2 seconds.
         */
        public Builder autoPollRateInSeconds(int autoPollRateInSeconds) {
            if(autoPollRateInSeconds < 2)
                throw new IllegalArgumentException("autoPollRateInSeconds cannot be less than 2 seconds");

            this.autoPollRateInSeconds = autoPollRateInSeconds;
            return this;
        }

        /**
         * Builds the configured {@link AutoPollingPolicy} instance.
         *
         * @param configFetcher the internal config fetcher.
         * @param cache the internal cache.
         * @return the configured {@link AutoPollingPolicy} instance
         */
        public AutoPollingPolicy build(ConfigFetcher configFetcher, ConfigCache cache) {
            return new AutoPollingPolicy(configFetcher, cache, this);
        }
    }
}
