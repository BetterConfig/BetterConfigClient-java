package com.betterconfig;

import java.io.Closeable;

/**
 * Defines the public interface of the {@link BetterConfigClient}.
 */
public interface ConfigurationProvider extends Closeable {
    /**
     * Gets the configuration as a json string.
     *
     * @return the configuration as a json string. Returns {@code null} if the
     * configuration fetch from the network fails.
     */
    String getConfigurationJsonString();

    /**
     * Gets the configuration serialized to a {@code <T>} type.
     *
     * @param classOfT the class of T.
     * @param defaultValue in case of any failure, this value will be returned.
     * @param <T> the type of the desired object.
     * @return an object of type T containing the whole configuration.
     */
    <T> T getConfiguration(Class<T> classOfT, T defaultValue);

    /**
     * Gets a string value from the configuration identified by the given {@code key}.
     *
     * @param key the identifier of the configuration value.
     * @param defaultValue in case of any failure, this value will be returned.
     * @return the configuration value as a string identified by the given key.
     */
    String getStringValue(String key, String defaultValue);

    /**
     * Gets a boolean value from the configuration identified by the given {@code key}.
     *
     * @param key the identifier of the configuration value.
     * @param defaultValue in case of any failure, this value will be returned.
     * @return the configuration value as a boolean identified by the given key.
     */
    boolean getBooleanValue(String key, boolean defaultValue);

    /**
     * Gets an int value from the configuration identified by the given {@code key}.
     *
     * @param key the identifier of the configuration value.
     * @param defaultValue in case of any failure, this value will be returned.
     * @return the configuration value as an int identified by the given key.
     */
    int getIntegerValue(String key, int defaultValue);

    /**
     * Gets a double value from the configuration identified by the given {@code key}.
     *
     * @param key the identifier of the configuration value.
     * @param defaultValue in case of any failure, this value will be returned.
     * @return the configuration value as a double identified by the given key.
     */
    double getDoubleValue(String key, double defaultValue);

    /**
     * Invalidates the internal cache.
     */
    void invalidateCache();
}
