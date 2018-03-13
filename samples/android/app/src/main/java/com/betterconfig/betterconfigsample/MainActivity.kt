package com.betterconfig.betterconfigsample

import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.widget.TextView
import com.betterconfig.AutoPollingPolicy
import com.betterconfig.BetterConfigClient
import com.betterconfig.ConfigCache
import com.betterconfig.ConfigFetcher

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        BetterConfigClient.newBuilder()
                .refreshPolicy({fetcher: ConfigFetcher, cache: ConfigCache -> AutoPollingPolicy.newBuilder()
                        .autoPollRateInSeconds(5)
                        .configurationChangeListener({parser, newConfiguration ->
                            run {
                                var config = parser.parse(Sample::class.java, newConfiguration)
                                var textField = findViewById<TextView>(R.id.editText)
                                textField.text = "keyBool: " + config.keyBool + "\n" +
                                        "keyInteger: " + config.keyInteger + "\n" +
                                        "keyDouble: " + config.keyDouble + "\n" +
                                        "keyString: " + config.keyString
                            }
                        })
                        .build(fetcher, cache)})
                .build("samples/01")
    }

    data class Sample(val keyBool: Boolean = false, val keyInteger: Int = 0, val keyDouble: Double = 0.0, val keyString: String = "")
}
