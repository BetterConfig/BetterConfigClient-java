package com.betterconfig;

import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

public class AutoPollingPolicyTest {
    @Test
    public void getCacheFails() throws Exception {
        String result = "test";

        ConfigFetcher fetcher = mock(ConfigFetcher.class);
        ConfigCache cache = mock(ConfigCache.class);

        doThrow(new Exception()).when(cache).read();
        doThrow(new Exception()).when(cache).write(anyString());

        when(cache.get()).thenCallRealMethod();
        doCallRealMethod().when(cache).set(anyString());

        when(fetcher.getConfigurationJsonStringAsync())
                .thenReturn(CompletableFuture.completedFuture(new FetchResponse(FetchResponse.Status.Fetched, result)));

        AutoPollingPolicy policy = AutoPollingPolicy.newBuilder()
                .autoPollRateInSeconds(2)
                .build(fetcher,cache);

        assertEquals(result, policy.getConfigurationJsonAsync().get());
    }

    @Test
    public void getFetchedSameResponseNotUpdatesCache() throws Exception {
        String result = "test";

        ConfigFetcher fetcher = mock(ConfigFetcher.class);
        ConfigCache cache = mock(ConfigCache.class);

        when(cache.get()).thenReturn(result);

        when(fetcher.getConfigurationJsonStringAsync())
                .thenReturn(CompletableFuture.completedFuture(new FetchResponse(FetchResponse.Status.Fetched, result)));

        AutoPollingPolicy policy = AutoPollingPolicy.newBuilder()
                .autoPollRateInSeconds(2)
                .build(fetcher,cache);

        assertEquals("test", policy.getConfigurationJsonAsync().get());

        verify(cache, never()).write(result);
    }
}
