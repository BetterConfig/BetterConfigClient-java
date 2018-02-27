package com.betterconfig;

/**
 * This class represents a fetch response object.
 */
public class FetchResponse {
    public enum Status {
        Fetched,
        NotModified,
        Failed
    }

    private Status status;
    private String config;

    /**
     * Gets whether a new configuration value was fetched or not.
     *
     * @return true if a new configuration value was fetched, otherwise false.
     */
    public boolean isFetched() {
        return this.status == Status.Fetched;
    }

    /**
     * Gets whether the fetch resulted a '304 Not Modified' or not.
     *
     * @return true if the fetch resulted a '304 Not Modified' code, otherwise false.
     */
    public boolean isNotModified() {
        return this.status == Status.NotModified;
    }

    /**
     * Gets whether the fetch failed or not.
     *
     * @return true if the fetch is failed, otherwise false.
     */
    public boolean isFailed() {
        return this.status == Status.Failed;
    }

    /**
     * Gets the fetched configuration value, should be used when the response
     * has a {@code FetchResponse.Status.Fetched} status code.
     *
     * @return the fetched config.
     */
    public String config() {
        return this.config;
    }

    FetchResponse(Status status, String config) {
        this.status = status;
        this.config = config;
    }
}
