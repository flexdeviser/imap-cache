package org.e4s.application.config;

import com.hazelcast.config.Config;
import com.hazelcast.config.EvictionPolicy;
import com.hazelcast.config.InMemoryFormat;
import com.hazelcast.config.MapConfig;
import com.hazelcast.config.MaxSizePolicy;
import info.jerrinot.subzero.SubZero;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class HazelcastSettings {


    @Bean
    public Config hzConfig() {
        Config hz = new Config();

        SubZero.useAsGlobalSerializer(hz);

        MapConfig pqStore = new MapConfig("pq_on_heap");
        pqStore.setInMemoryFormat(InMemoryFormat.BINARY);
        pqStore.setBackupCount(0).setAsyncBackupCount(0);
        pqStore.getEvictionConfig()
                .setEvictionPolicy(EvictionPolicy.LFU)
                .setMaxSizePolicy(MaxSizePolicy.ENTRY_COUNT)
                .setSize(100000);

        return hz;
    }

}
