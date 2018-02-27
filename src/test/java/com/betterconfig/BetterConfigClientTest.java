package com.betterconfig;

import com.google.gson.Gson;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class BetterConfigClientTest {

    private static final String TOKEN = "TEST_TOKEN";
    private BetterConfigClient client;
    private MockWebServer server;

    @BeforeEach
    public void setUp() throws IOException {
        this.server = new MockWebServer();
        this.server.start();

        this.client = BetterConfigClient.Builder()
                .projectToken(TOKEN)
                .cache(configFetcher -> {
                    configFetcher.setUrl(this.server.url("/").toString());
                    return InMemoryConfigCache.Builder().cacheTimeoutInSeconds(2).asyncRefresh(true).build(configFetcher);
                })
                .build();
    }

    @AfterEach
    public void tearDown() throws IOException {
        this.client.close();
        this.server.shutdown();
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
    public void getStringValue() {
        String sValue = "ááúúóüüőőööúúűű";
        String result = "{ \"fakeKey\":\""+sValue+"\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        String config = this.client.getStringValue("fakeKey", null);
        assertEquals(sValue, config);
    }

    @Test
    public void getStringValueReturnsDefaultOnFail() {
        String def = "def";
        server.enqueue(new MockResponse().setResponseCode(500));
        String config = this.client.getStringValue("fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getBooleanValue() {
        String result = "{ \"fakeKey\":\"true\" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        boolean config = this.client.getBooleanValue("fakeKey", false);
        assertTrue(config);
    }

    @Test
    public void getBooleanValueReturnsDefaultOnFail() {
        server.enqueue(new MockResponse().setResponseCode(500));
        boolean config = this.client.getBooleanValue("fakeKey", true);
        assertTrue(config);
    }

    @Test
    public void getIntegerValue() {
        int iValue = 342423;
        String result = "{ \"fakeKey\":"+iValue+" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        int config = this.client.getIntegerValue("fakeKey", 0);
        assertEquals(iValue, config);
    }

    @Test
    public void getIntegerValueReturnsDefaultOnFail() {
        int def = 342423;
        server.enqueue(new MockResponse().setResponseCode(500));
        int config = this.client.getIntegerValue("fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void getDoubleValue() {
        double iValue = 432.234;
        String result = "{ \"fakeKey\":"+iValue+" }";
        server.enqueue(new MockResponse().setResponseCode(200).setBody(result));
        double config = this.client.getDoubleValue("fakeKey", 0.0);
        assertEquals(iValue, config);
    }

    @Test
    public void getDoubleValueReturnsDefaultOnFail() {
        double def = 432.234;
        server.enqueue(new MockResponse().setResponseCode(500));
        double config = this.client.getDoubleValue("fakeKey", def);
        assertEquals(def, config);
    }

    @Test
    public void invalidateCache() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        server.enqueue(new MockResponse().setResponseCode(200).setBody("test2"));

        assertEquals("test", this.client.getConfigurationJsonString());
        this.client.invalidateCache();
        assertEquals("test2", this.client.getConfigurationJsonString());
    }

    @Test
    public void invalidateCacheFail() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("test"));
        server.enqueue(new MockResponse().setResponseCode(500));

        assertEquals("test", this.client.getConfigurationJsonString());
        this.client.invalidateCache();
        assertEquals(null, this.client.getConfigurationJsonString());
    }

    class Sample {
        int value1 = 1;
        String value2 = "abc";
        double value3 = 2.4;
        boolean value4 = true;
    }
}