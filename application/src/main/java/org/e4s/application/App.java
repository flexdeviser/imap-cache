package org.e4s.application;

import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import org.apache.commons.io.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.text.DecimalFormat;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@SpringBootApplication
public class App implements CommandLineRunner {

    public static void main(String[] args) {
        SpringApplication.run(App.class).start();
    }

    @Autowired
    private HazelcastInstance instance;

    @Override
    public void run(String... args) throws Exception {

        final IMap<UUID, byte[]> onHeap = instance.getMap("pq_on_heap");

        Executors.newSingleThreadScheduledExecutor().scheduleAtFixedRate(()->{
            System.out.println("Count: " + onHeap.getLocalMapStats().getOwnedEntryCount()+ " Memory cost: " + getMemoryUnit(onHeap.getLocalMapStats().getOwnedEntryMemoryCost()) + " Total Cost: " + getMemoryUnit(onHeap.getLocalMapStats().getHeapCost()));
        }, 0, 10, TimeUnit.SECONDS);

    }


    public static String getMemoryUnit(long bytes) {
        DecimalFormat oneDecimal = new DecimalFormat("0.0");
        float BYTE = 1024.0f, KB = BYTE, MB = KB * KB, GB = MB * KB, TB = GB * KB;
        long absNumber = Math.abs(bytes);
        double result = bytes;
        String suffix = " Bytes";
        if (absNumber < MB) {
            result = bytes / KB;
            suffix = " KB";
        } else if (absNumber < GB) {
            result = bytes / MB;
            suffix = " MB";
        } else if (absNumber < TB) {
            result = bytes / GB;
            suffix = " GB";
        }
        return oneDecimal.format(result) + suffix;
    }
}
