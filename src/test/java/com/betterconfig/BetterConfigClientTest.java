package com.betterconfig;

import com.google.gson.Gson;
import jdk.nashorn.internal.parser.Token;
import okhttp3.OkHttpClient;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import sun.java2d.pipe.SpanShapeRenderer;

import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.junit.jupiter.api.Assertions.*;

public class BetterConfigClientTest {

    private static final String SECRET = "TEST_SECRET";
    private BetterConfigClient client;
    private MockWebServer server;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.client = BetterConfigClient.newBuilder()
                .httpClient(new OkHttpClient.Builder().build())
                .refreshPolicy((configFetcher, cache) -> {
                    configFetcher.setUrl(this.server.url("/").toString());
                    return ExpiringCachePolicy.newBuilder().cacheRefreshRateInSeconds(2).asyncRefresh(true).build(configFetcher, cache);
                })
                .build(SECRET);
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.client.close();
        this.server.shutdown();
    }

    @Test
    public void ensuresProjectSecretIsNotNull() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> new BetterConfigClient(null));

        assertEquals("projectSecret is null or empty", exception.getMessage());

        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> BetterConfigClient.newBuilder().build(null));

        assertEquals("projectSecret is null or empty", builderException.getMessage());
    }

    @Test
    public void ensuresProjectSecretIsNotEmpty() {
        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class, () -> new BetterConfigClient(""));

        assertEquals("projectSecret is null or empty", exception.getMessage());

        IllegalArgumentException builderException = assertThrows(
                IllegalArgumentException.class, () -> BetterConfigClient.newBuilder().build(""));

        assertEquals("projectSecret is null or empty", builderException.getMessage());
    }

    @Test
    public void getConfigurationJsonStringWithDefaultConfigTimout() {
        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        // makes a call to a real url which would fail, null expected
        String config = cl.getConfigurationJsonString();
        assertEquals(null, config);
    }

    @Test
    public void getConfigurationJsonWithDefaultConfigTimout() {
        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        // makes a call to a real url which would fail, default expected
        Sample config = cl.getConfiguration(Sample.class, Sample.Empty);
        assertEquals(Sample.Empty, config);
    }

    @Test
    public void getValueWithDefaultConfigTimout() {
        BetterConfigClient cl = BetterConfigClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2)
                .build(SECRET);

        // makes a call to a real url which would fail, default expected
        boolean config = cl.getValue(Boolean.class, "key", true);
        assertTrue(config);
    }

    @Test
    public void getConfigurationJsonString() {
        String result = "{ \"fakeKey\":\"fakeValue\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));

        String config = this.client.getConfigurationJsonString();
        assertEquals(result, config);
    }

    @Test
    public void getConfigurationJsonStringFailToRefresh() {
        server.enqueue(new MockResponse().setResponseCode(500));

        String config = this.client.getConfigurationJsonString();
        assertNull(config);
    }

    @Test
    public void getConfiguration() {
        Sample sample = new Sample();
        Gson gson = new Gson();
        String json = gson.toJson(sample);
        server.enqueue(new MockResponse().setResponseCode(200).setBody(json));

        Sample result = this.client.getConfiguration(Sample.class,null);
        assertEquals(sample.value1, result.value1);
        assertEquals(sample.value2, result.value2);
        assertEquals(sample.value3, result.value3);
        assertEquals(sample.value4, result.value4);
    }

    @Test
    public void getConfigurationReturnsDefaultOnFail() {
        Sample sample = new Sample();
        server.enqueue(new MockResponse().setResponseCode(500));

        Sample result = this.client.getConfiguration(Sample.class,sample);
        assertSame(sample, result);
    }

    @Test
    public void getConfigurationReturnsDefaultOnException() {
        String badJson = "{ test: test] }";
        Sample def = new Sample();
        server.enqueue(new MockResponse().setResponseCode(200).setBody(badJson));

        Sample result = this.client.getConfiguration(Sample.class,def);
        assertSame(def, result);
    }

    @Test
    public void getStringValue() {
        String sValue = "ááúúóüüőőööúúűű";
        String result = "{ \"fakeKey\":\""+sValue+"\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        String config = this.client.getValue(String.class,"fakeKey", null);
        assertEquals(sValue, config);
    }

    @Test
    public void getStringValueReturnsDefaultOnFail() {
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(500));
        String config = this.client.getValue(String.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getStringValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        String config = this.client.getValue(String.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getBooleanValue() {
        String result = "{ \"fakeKey\":\"true\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        boolean config = this.client.getValue(Boolean.class,"fakeKey", false);
        assertTrue(config);
    }

    @Test
    public void getBooleanValueReturnsDefaultOnFail() {
        server.enqueue(new MockResponse().setResponseCode(500));
        boolean config = this.client.getValue(boolean.class,"fakeKey", true);
        assertTrue(config);
    }

    @Test
    public void getBooleanValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        boolean def = true;
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        boolean config = this.client.getValue(boolean.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getIntegerValue() {
        int iValue = 342423;
        String result = "{ \"fakeKey\":"+iValue+" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        int config = this.client.getValue(Integer.class,"fakeKey", 0);
        assertEquals(iValue, config);
    }

    @Test
    public void getIntegerValueReturnsDefaultOnFail() {
        int def = 342423;
        server.enqueue(new MockResponse().setResponseCode(500));
        int config = this.client.getValue(int.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getIntegerValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        int def = 14;
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        int config = this.client.getValue(int.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getDoubleValue() {
        double iValue = 432.234;
        String result = "{ \"fakeKey\":"+iValue+" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        double config = this.client.getValue(Double.class,"fakeKey", 0.0);
        assertEquals(iValue, config);
    }

    @Test
    public void getDoubleValueReturnsDefaultOnFail() {
        double def = 432.234;
        server.enqueue(new MockResponse().setResponseCode(500));
        double config = this.client.getValue(double.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getDoubleValueReturnsDefaultOnException() {
        String result = "{ test: test] }";
        double def = 14.5;
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        double config = this.client.getValue(double.class,"fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void invalidateCache() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("test2"));

        assertEquals("test", this.client.getConfigurationJsonString());
        this.client.forceRefresh();
        assertEquals("test2", this.client.getConfigurationJsonString());
    }

    @Test
    public void invalidateCacheFail() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        server.enqueue(new MockResponse().setResponseCode(500));

        assertEquals("test", this.client.getConfigurationJsonString());
        this.client.forceRefresh();
        assertEquals("test", this.client.getConfigurationJsonString());
    }

    static class Sample {
        static Sample Empty = new Sample();
        int value1 = 1;
        String value2 = "abc";
        double value3 = 2.4;
        boolean value4 = true;
    }
}