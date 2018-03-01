import com.betterconfig.BetterConfigClient;
import com.betterconfig.ConfigurationProvider;

public class Main {
    public static void main(String[] args) {
        ConfigurationProvider config = BetterConfigClient
                .newBuilder()
                .build("samples/01");

        System.out.println(config.getStringValue("keySampleText", ""));
        System.out.println(config.getStringValue("myKeyNotExits", "This text is from the default due to requesting a non existing config value."));
    }
}
