import com.betterconfig.BetterConfigClient;

public class Main {
    public static void main(String[] args) {
        BetterConfigClient client = BetterConfigClient
                .newBuilder()
                .build("samples/01");

        // get the configuration serialized to an object
        SampleConfig config = client.getConfiguration(SampleConfig.class, SampleConfig.Empty);

        System.out.println("keyBool: " + config.keyBool);
        System.out.println("keyDouble: " + config.keyDouble);
        System.out.println("keyInteger: " + config.keyInteger);
        System.out.println("keyString: " + config.keyString);

        // get individual config values identified by a key
        System.out.println("keySampleText: " + client.getValue(String.class,"keySampleText", ""));
    }

    static class SampleConfig {
        static SampleConfig Empty = new SampleConfig();

        boolean keyBool;
        double keyDouble;
        int keyInteger;
        String keyString;
    }
}
