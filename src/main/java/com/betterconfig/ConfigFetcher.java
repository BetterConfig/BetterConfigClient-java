package com.betterconfig;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;

/**
 * This class is used by the internal {@link ConfigCache} implementation to fetch the latest configuration.
 */
public class ConfigFetcher implements Closeable {
    private static final Logger logger = LoggerFactory.getLogger(ConfigFetcher.class);
    private OkHttpClient httpClient;
    private String url;
    private String eTag;

    void setUrl(String url) {
        this.url = url;
    }

    /**
     * Constructs a new instance.
     *
     * @param httpClient the http client.
     * @param projectToken the project token.
     */
    public ConfigFetcher(OkHttpClient httpClient, String projectToken) {
        this.httpClient = httpClient;
        this.url = "https://cdn.betterconfig.com/configuration-files/" + projectToken + "/config.json";
    }

    /**
     * Gets the latest configuration from the network.
     *
     * @return a {@link FetchResponse} instance which holds the result of the fetch.
     */
    public FetchResponse getConfigurationJsonString() {
        Request request = this.getRequest();

        Response response = null;
        try {
            response = this.httpClient.newCall(request).execute();
            if(response.isSuccessful())
            {
                this.eTag = response.header("ETag");
                return new FetchResponse(FetchResponse.Status.Fetched, response.body().string());
            }

            if(response.code() == 304)
                return new FetchResponse(FetchResponse.Status.NotModified, null);

            logger.debug("Non success status code:" + response.code());
        } catch (IOException e) {
            logger.error("An error occurred during fetching the latest configuration.", e);
        } finally {
            if(response != null)
                response.close();
        }

        return new FetchResponse(FetchResponse.Status.Failed, null);
    }

    @Override
    public void close() throws IOException {
        if (this.httpClient != null) {
            if (this.httpClient.dispatcher() != null && this.httpClient.dispatcher().executorService() != null)
                this.httpClient.dispatcher().executorService().shutdownNow();

            if (this.httpClient.connectionPool() != null)
                this.httpClient.connectionPool().evictAll();

            if (this.httpClient.cache() != null)
                this.httpClient.cache().close();

        }
    }

    Request getRequest() {
        Request.Builder builder =  new Request.Builder()
                .addHeader("User-Agent", "BetterConfigClient-Java/" + "1.0");

        if(this.eTag != null)
            builder.addHeader("If-None-Match", this.eTag);

        return builder.url(this.url).build();
    }
}

