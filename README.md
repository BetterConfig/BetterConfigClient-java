# BetterConfig client for Java
BetterConfig is a cloud based configuration as a service. It integrates with your apps, backends, websites, 
and other programs, so you can configure them through [this](https://betterconfig.com) website even after they are deployed.

[![Build Status](https://travis-ci.org/BetterConfig/BetterConfigClient-java.svg?branch=master)](https://travis-ci.org/BetterConfig/BetterConfigClient-java)
[![Maven Central](https://maven-badges.herokuapp.com/maven-central/com.betterconfig/betterconfig-client/badge.svg)](https://maven-badges.herokuapp.com/maven-central/com.betterconfig/betterconfig-client)
[![Coverage Status](https://img.shields.io/codecov/c/github/BetterConfig/BetterConfigClient-java.svg)](https://codecov.io/gh/BetterConfig/BetterConfigClient-java)
[![Javadocs](http://javadoc.io/badge/com.betterconfig/betterconfig-client.svg)](http://javadoc.io/doc/com.betterconfig/betterconfig-client)

## Getting started

**1. Add the package to your project**

*Maven:*
```xml
<dependency>
    <groupId>com.betterconfig</groupId>
    <artifactId>betterconfig-client</artifactId>
    <version>1.1.0</version>
</dependency>
```
*Gradle:*
```groovy
compile 'com.betterconfig:betterconfig-client:1.1.0'
```
**2. Get your Project Secret from [BetterConfig.com](https://betterconfig.com) portal**
![YourConnectionUrl](https://raw.githubusercontent.com/BetterConfig/BetterConfigClient-dotnet/master/media/readme01.png  "YourProjectToken")

**3. Import the BetterConfig package**
```java
import com.betterconfig.*;
```

**4. Create a BetterConfigClient instance**
```java
BetterConfigClient client = new BetterConfigClient("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
**5. Get your config value**
```java
boolean isMyAwesomeFeatureEnabled = client.getValue(Boolean.class, "key-of-my-awesome-feature", false);
if(isMyAwesomeFeatureEnabled) {
    //show your awesome feature to the world!
}
```
Or use the async APIs:
```java
client.getValueAsync(Boolean.class, "key-of-my-awesome-feature", false)
    .thenAccept(isMyAwesomeFeatureEnabled -> {
         //show your awesome feature to the world!
    });
```

## Android
The minimum supported sdk version is 26 (oreo). Java 1.8 or later is required.
```groovy
android {
    defaultConfig {
        //...
        minSdkVersion 26
    }
    
    compileOptions {
        sourceCompatibility JavaVersion.VERSION_1_8
        targetCompatibility JavaVersion.VERSION_1_8
    }
}
```

## Configuration
### HttpClient
The BetterConfig client internally uses an [OkHttpClient](https://github.com/square/okhttp) instance to fetch the latest configuration over HTTP. You have the option to override the internal HttpClient with your customized one. For example if your application runs behind a proxy you can do the following:
```java
Proxy proxy = new Proxy(Proxy.Type.HTTP, new InetSocketAddress("proxyHost", proxyPort));

BetterConfigClient client = BetterConfigClient.newBuilder()
                .httpClient(new OkHttpClient.Builder()
                            .proxy(proxy)
                            .build())
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
> As the BetterConfig client maintains the whole lifetime of the internal HttpClient, it's being closed simultaneously with the BetterConfig client, refrain from closing the HttpClient manually.

### Refresh policies
The internal caching control and the communication between the client and BetterConfig are managed through a refresh policy. There are 3 predefined implementations built in the library.
#### 1. Auto polling policy (default)
This policy fetches the latest configuration and updates the cache repeatedly. 
##### Poll rate 
You have the option to configure the polling interval through its builder (it has to be greater than 2 seconds, the default is 60):
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> 
                    AutoPollingPolicy.newBuilder()
                        .autoPollRateInSeconds(120) // set the polling interval
                        .build(configFetcher, cache)
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
##### Change listeners 
You can set change listeners that will be notified when a new configuration is fetched. The policy calls the listeners only, when the new configuration is differs from the cached one.
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> 
                    AutoPollingPolicy.newBuilder()
                        .configurationChangeListener((parser, newConfiguration) -> {
                            // here you can parse the new configuration like this: 
                            // parser.parseValue(Boolean.class, newConfiguration, "key-of-my-awesome-feature")                            
                        })
                        .build(configFetcher, cache)
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
If you want to subscribe to the configuration changed event later in your applications lifetime, then you can do the following (this will only work when you have an auto polling refresh policy configured in the BetterConfig client):
```java
client.getRefreshPolicy(AutoPollingPolicy.class)
    .addConfigurationChangeListener((parser, newConfiguration) -> {
        // here you can parse the new configuration like this: 
        // parser.parseValue(Boolean.class, newConfiguration, "key-of-my-awesome-feature")  
    });
```

#### 2. Expiring cache policy
This policy uses an expiring cache to maintain the internally stored configuration. 
##### Cache refresh interval 
You can define the refresh rate of the cache in seconds, 
after the initial cached value is set this value will be used to determine how much time must pass before initiating a new configuration fetch request through the `ConfigFetcher`.
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> 
                    ExpiringCachePolicy.newBuilder()
                        .cacheRefreshRateInSeconds(120) // the cache will expire in 120 seconds
                        .build(configFetcher, cache)
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
##### Async / Sync refresh
You can define how do you want to handle the expiration of the cached configuration. If you choose asynchronous refresh then 
when a request is being made on the cache while it's expired, the previous value will be returned immediately 
until the fetching of the new configuration is completed.
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> 
                    ExpiringCachePolicy.newBuilder()
                        .asyncRefresh(true) // the refresh will be executed asynchronously
                        .build(configFetcher, cache)
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
If you set the `.asyncRefresh()` to be `false`, the refresh operation will be awaited
until the fetching of the new configuration is completed.

#### 3. Always fetching policy
With this policy every new configuration request on the BetterConfigClient will trigger a new fetch over HTTP.
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> new AlwaysFetchingPolicy(configFetcher,cache));
```

#### Custom Policy
You can also implement your custom refresh policy by extending the `RefreshPolicy` abstract class.
```java
public class MyCustomPolicy extends RefreshPolicy {
    
    public MyCustomPolicy(ConfigFetcher configFetcher, ConfigCache cache) {
        super(configFetcher, cache);
    }

    @Override
    public CompletableFuture<String> getConfigurationJsonAsync() {
        // this method will be called when the configuration is requested from the BetterConfig client.
        // you can access the config fetcher through the super.fetcher() and the internal cache via super.cache()
    }
    
    // optional, in case if you have any resources that should be closed
    @Override
     public void close() throws IOException {
        super.close();
        // here you can close your resources
    }
}
```
> If you decide to override the `close()` method, you also have to call the `super.close()` to tear down the cache appropriately.

Then you can simply inject your custom policy implementation into the BetterConfig client:
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .refreshPolicy((configFetcher, cache) -> new MyCustomPolicy(configFetcher, cache)) // inject your custom policy
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```

### Custom Cache
You have the option to inject your custom cache implementation into the client. All you have to do is to inherit from the `ConfigCache` abstract class:
```java
public class MyCustomCache extends ConfigCache {
    
    @Override
    public String read() {
        // here you have to return with the cached value
        // you can access the latest cached value in case 
        // of a failure like: super.inMemoryValue();
    }

    @Override
    public void write(String value) {
        // here you have to store the new value in the cache
    }
}
```

Using your custom cache implementation:
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .cache(new MyCustomCache()) // inject your custom cache
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```

### Maximum wait time for synchronous calls
You have the option to set a timeout value for the synchronous methods of the library (`getConfigurationJsonString()`, `getConfiguration()`, `getValue()` etc.) which means
when a sync call takes longer than the timeout value, it'll return with the default.
```java
BetterConfigClient client = BetterConfigClient.newBuilder()
                .maxWaitTimeForSyncCallsInSeconds(2) // set the max wait time
                .build("<PLACE-YOUR-PROJECT-SECRET-HERE>");
```
### Force refresh
Any time you want to refresh the cached configuration with the latest one, you can call the `forceRefresh()` on the client.
This will make a fetch and will update the local cache.

## Logging
The BetterConfig client uses the [slf4j](https://www.slf4j.org)'s facade for logging.
