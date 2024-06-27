package org.e4s.app;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.apache.commons.lang3.time.DurationFormatUtils;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.core.io.Resource;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadFactory;
import java.util.stream.IntStream;

@SpringBootApplication
public class LoadingApp implements CommandLineRunner {


    public static void main(String[] args) {
        SpringApplication.run(LoadingApp.class).start();
    }

    private final Timer fetchTimer;

    private final MeterRegistry registry;

    public LoadingApp(MeterRegistry registry) {
        this.registry = registry;
        this.fetchTimer = registry.timer("api", "cache", "fetch");
    }

    @Value("classpath:v4_uuids.txt")
    Resource resource;

    @Override
    public void run(String... args) throws Exception {

        Logger LOG = LoggerFactory.getLogger("http-client");

        // create uuids
        if (resource.getFile().length() == 0) {
            FileWriter fw = new FileWriter(resource.getFile());
            BufferedWriter bw = new BufferedWriter(fw);

            IntStream.range(0, 10000).forEach(i -> {
                try {
                    bw.write(String.valueOf(UUID.randomUUID()));
                    bw.newLine();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            });

            bw.flush();
            bw.close();
        }

        PoolingHttpClientConnectionManager poolingConnManager = new PoolingHttpClientConnectionManager();
        poolingConnManager.setMaxTotal(50);
        poolingConnManager.setDefaultMaxPerRoute(30);

        // start 30 threads to loading data
        List<String> uuids = Files.readAllLines(Path.of(resource.getURI()));

        ExecutorService runService = Executors.newFixedThreadPool(5, new ThreadFactory() {

            private int counter = 0;
            private String prefix = "http-client";

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, prefix + "-" + counter++);
            }
        });

        // split to 5 chunks
        Lists.partition(uuids, 100).forEach(ids -> {
            final CloseableHttpClient client = HttpClients.custom()
                    .setConnectionManager(poolingConnManager)
                    .build();
            final ObjectMapper mapper = new ObjectMapper();
            // 100 uuid in a job
            runService.submit(() -> {

                for (int i = 0; i < ids.size(); i++) {
                    final HttpGet request = new HttpGet("http://localhost:8080/store?id=" + ids.get(i));

                    int finalI = i;
                    fetchTimer.record(() -> {
                        long start = System.currentTimeMillis();
                        try (CloseableHttpResponse resp = client.execute(request)) {
                            List<Map> result = mapper.readValue(resp.getEntity().getContent(), new TypeReference<>() {
                            });

                            LOG.info("index: {}, id: {}, size: {}, took: {}", finalI, ids.get(finalI), result.size(), DurationFormatUtils.formatDurationHMS(System.currentTimeMillis() - start));

                        } catch (IOException e) {
                            LOG.error("CAUGHT EXCEPTION", e);
                        }
                    });
                }
            });

        });


    }
}
