package com.betterconfig;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import okhttp3.OkHttpClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;

/**
 * A client for handling configurations provided by BetterConfig.
 */
public class BetterConfigClient implements ConfigurationProvider {
    private static final Logger logger = LoggerFactory.getLogger(BetterConfigClient.class);
    private final RefreshPolicy refreshPolicy;
    private final Gson gson = new Gson();
    private final JsonParser parser = new JsonParser();
    private final int maxWaitTimeForSyncCallsInSeconds;

    private BetterConfigClient(String projectSecret, Builder builder) throws IllegalArgumentException {
        if(projectSecret == null || projectSecret.isEmpty())
            throw new IllegalArgumentException("projectSecret is null or empty");

        this.maxWaitTimeForSyncCallsInSeconds = builder.maxWaitTimeForSyncCallsInSeconds;

        ConfigFetcher fetcher = new ConfigFetcher(builder.httpClient == null
                ? new OkHttpClient
                    .Builder()
                    .retryOnConnectionFailure(true)
                    .build()
                : builder.httpClient, projectSecret);

        ConfigCache cache = builder.cache == null
                ? new InMemoryConfigCache()
                : builder.cache;

        this.refreshPolicy = builder.refreshPolicy == null
                ? AutoPollingPolicy.newBuilder()
                    .build(fetcher, cache)
                : builder.refreshPolicy.apply(fetcher, cache);
    }

    /**
     * Constructs a new client instance with the default configuration.
     *
     * @param projectSecret the token which identifies your project configuration.
     */
    public BetterConfigClient(String projectSecret) {
        this(projectSecret, newBuilder());
    }

    @Override
    public String getConfigurationJsonString() {
        try {
            return this.maxWaitTimeForSyncCallsInSeconds > 0
                    ? this.getConfigurationJsonStringAsync().get(this.maxWaitTimeForSyncCallsInSeconds, TimeUnit.SECONDS)
                    : this.getConfigurationJsonStringAsync().get();
        } catch (Exception e) {
            logger.error("An error occurred during reading the configuration.", e);
            return null;
        }
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonStringAsync() {
        return this.refreshPolicy.getConfigurationJsonAsync();
    }

    @Override
    public <T> T getConfiguration(Class<T> classOfT, T defaultValue) {
        try {
            return this.maxWaitTimeForSyncCallsInSeconds > 0
                    ? this.getConfigurationAsync(classOfT, defaultValue).get(this.maxWaitTimeForSyncCallsInSeconds, TimeUnit.SECONDS)
                    : this.getConfigurationAsync(classOfT, defaultValue).get();
        } catch (Exception e) {
            logger.error("An error occurred during deserialization.", e);
            return defaultValue;
        }
    }

    @Override
    public <T> CompletableFuture<T> getConfigurationAsync(Class<T> classOfT, T defaultValue) {
        return this.refreshPolicy.getConfigurationJsonAsync()
                .thenApply(config -> config == null
                        ? defaultValue
                        : gson.fromJson(config, classOfT));
    }

    @Override
    public  <T> T getValue(Class<T> classOfT, String key, T defaultValue) {
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        try {
            return this.maxWaitTimeForSyncCallsInSeconds > 0
                    ? this.getValueAsync(classOfT, key, defaultValue).get(this.maxWaitTimeForSyncCallsInSeconds, TimeUnit.SECONDS)
                    : this.getValueAsync(classOfT, key, defaultValue).get();
        } catch (Exception e) {
            logger.error("An error occurred during the reading of the value for key '"+key+"'.", e);
            return defaultValue;
        }
    }

    @Override
    public <T> CompletableFuture<T> getValueAsync(Class<T> classOfT, String key, T defaultValue) {
        if(key == null || key.isEmpty())
            throw new IllegalArgumentException("key is null or empty");

        return this.getJsonElementAsync(key)
                .thenApply(element -> {
                    if(element == null) return defaultValue;

                    if(classOfT == String.class)
                        return classOfT.cast(element.getAsString());
                    else if(classOfT == Integer.class)
                        return classOfT.cast(element.getAsInt());
                    else if(classOfT == Double.class)
                        return classOfT.cast(element.getAsDouble());
                    else if (classOfT == Boolean.class)
                        return classOfT.cast(element.getAsBoolean());
                    else throw new IllegalArgumentException("Only String, Integer, Double or Boolean types are supported");
                });
    }

    @Override
    public void forceRefresh() {
        try {
            this.forceRefreshAsync().get();
        } catch (Exception e) {
            logger.error("An error occurred during the refresh.", e);
        }
    }

    @Override
    public CompletableFuture<Void> forceRefreshAsync() {
        return this.refreshPolicy.refreshAsync();
    }

    @Override
    public void close() throws IOException {
        this.refreshPolicy.close();
    }

    private CompletableFuture<JsonElement> getJsonElementAsync(String key) {
        return this.refreshPolicy.getConfigurationJsonAsync()
                .thenApply(config -> config == null
                        ? null
                        : this.parser.parse(config).getAsJsonObject().get(key));
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
     * A builder that helps construct a {@link BetterConfigClient} instance.
     */
    public static class Builder {
        private OkHttpClient httpClient;
        private ConfigCache cache;
        private int maxWaitTimeForSyncCallsInSeconds;
        private BiFunction<ConfigFetcher, ConfigCache, RefreshPolicy> refreshPolicy;

        /**
         * Sets the underlying http client which will be used to fetch the latest configuration.
         *
         * @param httpClient the http client.
         * @return the builder.
         */
        public Builder httpClient(OkHttpClient httpClient) {
            this.httpClient = httpClient;
            return this;
        }

        /**
         * Sets the internal cache implementation.
         *
         * @param cache a {@link ConfigFetcher} implementation used to cache the configuration.
         * @return the builder.
         */
        public Builder cache(ConfigCache cache) {
            this.cache = cache;
            return this;
        }

        /**
         * Sets the internal refresh policy implementation.
         *
         * @param refreshPolicy a function used to create the a {@link RefreshPolicy} implementation with the given {@link ConfigFetcher} and {@link ConfigCache}.
         * @return the builder.
         */
        public Builder refreshPolicy(BiFunction<ConfigFetcher, ConfigCache, RefreshPolicy> refreshPolicy) {
            this.refreshPolicy = refreshPolicy;
            return this;
        }

        /**
         * Sets the maximum time in seconds at most how long the synchronous calls
         * e.g. {@code client.getConfiguration(...)} have to be blocked.
         *
         * @param maxWaitTimeForSyncCallsInSeconds the maximum time in seconds at most how long the synchronous calls
         *                                        e.g. {@code client.getConfiguration(...)} have to be blocked.
         * @return the builder.
         * @throws IllegalArgumentException when the given value is lesser than 2.
         */
        public Builder maxWaitTimeForSyncCallsInSeconds(int maxWaitTimeForSyncCallsInSeconds) {
            if(maxWaitTimeForSyncCallsInSeconds < 2)
                throw new IllegalArgumentException("maxWaitTimeForSyncCallsInSeconds cannot be less than 2 seconds");

            this.maxWaitTimeForSyncCallsInSeconds = maxWaitTimeForSyncCallsInSeconds;
            return this;
        }

        /**
         * Builds the configured {@link BetterConfigClient} instance.
         *
         * @param projectSecret the project token.
         * @return the configured {@link BetterConfigClient} instance.
         */
        public BetterConfigClient build(String projectSecret) {
            return new BetterConfigClient(projectSecret, this);
        }
    }
}