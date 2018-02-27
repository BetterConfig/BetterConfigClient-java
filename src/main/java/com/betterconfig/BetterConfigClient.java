package com.betterconfig;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import okhttp3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.function.Function;

/**
 * A client for using configurations provided by BetterConfig.
 */
public class BetterConfigClient implements ConfigurationProvider {
    private static final Logger logger = LoggerFactory.getLogger(BetterConfigClient.class);
    private ConfigCache cache;

    private BetterConfigClient(Builder builder) throws IllegalArgumentException {
        if(builder.projectToken == null)
            throw new IllegalArgumentException("projectToken");

        ConfigFetcher fetcher = new ConfigFetcher(builder.httpClient == null
                ? new OkHttpClient
                    .Builder()
                    .retryOnConnectionFailure(true)
                    .build()
                : builder.httpClient, builder.projectToken);

        this.cache = builder.cache == null
                ? InMemoryConfigCache
                    .Builder()
                    .cacheTimeoutInSeconds(2)
                    .asyncRefresh(true)
                    .build(fetcher)
                : builder.cache.apply(fetcher);
    }

    /**
     * Constructs a new client with the default configuration.
     *
     * @param projectToken the token which identifies your project configuration.
     */
    public BetterConfigClient(String projectToken) {
        this(Builder().projectToken(projectToken));
    }

    @Override
    public String getConfigurationJsonString() {
        return this.cache.get();
    }

    @Override
    public <T> T getConfiguration(Class<T> classOfT, T defaultValue) {
        String config = this.cache.get();
        if(config == null) return defaultValue;

        try {
            return new Gson().fromJson(config, classOfT);
        } catch (Exception e) {
            logger.error("An error occurred during deserialization.", e);
        }

        return defaultValue;
    }

    @Override
    public String getStringValue(String key, String defaultValue) {
        try {
            JsonObject obj = this.getJsonObject();
            if(obj == null) return defaultValue;
            return obj.get(key).getAsString();
        } catch (Exception e) {
            logger.error("An error occurred during deserialization.", e);
        }

        return defaultValue;
    }

    @Override
    public boolean getBooleanValue(String key, boolean defaultValue) {
        try {
            JsonObject obj = this.getJsonObject();
            if(obj == null) return defaultValue;
            return obj.get(key).getAsBoolean();
        } catch (Exception e) {
            logger.error("An error occurred during deserialization.", e);
        }

        return defaultValue;
    }

    @Override
    public int getIntegerValue(String key, int defaultValue) {
        try {
            JsonObject obj = this.getJsonObject();
            if(obj == null) return defaultValue;
            return obj.get(key).getAsInt();
        } catch (Exception e) {
            logger.error("An error occurred during deserialization.", e);
        }

        return defaultValue;
    }

    @Override
    public double getDoubleValue(String key, double defaultValue) {
        try {
            JsonObject obj = this.getJsonObject();
            if(obj == null) return defaultValue;
            return obj.get(key).getAsDouble();
        } catch (Exception e) {
            logger.error("An error occurred during deserialization.", e);
        }

        return defaultValue;
    }

    @Override
    public void invalidateCache() {
        this.cache.invalidateCache();
    }

    private JsonObject getJsonObject() {
        String config = this.cache.get();
        if(config == null) return null;

        JsonParser parser = new JsonParser();
        return parser.parse(config).getAsJsonObject();
    }

    @Override
    public void close() throws IOException {
        this.cache.close();
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
     * A builder that helps construct a {@link BetterConfigClient} instance.
     */
    public static class Builder {
        private OkHttpClient httpClient;
        private Function<ConfigFetcher, ConfigCache> cache;
        private String projectToken;

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
         * Sets the underlying cache implementation.
         *
         * @param cache a function used to create the cache with the given {@link ConfigFetcher}.
         * @return the builder.
         */
        public Builder cache(Function<ConfigFetcher, ConfigCache> cache) {
            this.cache = cache;
            return this;
        }

        /**
         * Sets the project token used to identify the current BetterConfig project.
         *
         * @param projectToken the project token.
         * @return the builder.
         */
        public Builder projectToken(String projectToken) {
            this.projectToken = projectToken;
            return this;
        }

        /**
         * Builds the configured {@link BetterConfigClient} instance.
         *
         * @return the configured {@link BetterConfigClient} instance.
         */
        public BetterConfigClient build() {
            return new BetterConfigClient(this);
        }
    }
}