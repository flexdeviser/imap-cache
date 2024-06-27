package org.e4s.api;


import com.hazelcast.core.HazelcastInstance;
import com.hazelcast.map.IMap;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import org.e4s.model.DeviceKey;
import org.e4s.model.PQ;
import org.redisson.codec.Kryo5Codec;
import org.redisson.codec.LZ4Codec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.IntStream;


@RestController
@SpringBootApplication
public class Api implements CommandLineRunner {


    final LZ4Codec coder = new LZ4Codec(new Kryo5Codec());


    public static void main(String[] args) {
        SpringApplication.run(Api.class).start();
    }

    @Autowired
    private HazelcastInstance instance;

    private final Timer imapTimer;

    public Api(MeterRegistry registry) {
        this.imapTimer = registry.timer("on_heap", "pq", "get");
    }

    @RequestMapping("/")
    String home() {
        return "HelloWorld";
    }


    @GetMapping(path = "/store", produces = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<List<PQ>> fetchData(@RequestParam("id") UUID key) throws IOException {

        List<PQ> result;

        final IMap<UUID, byte[]> onHeap = instance.getMap("pq_on_heap");

        long start = System.currentTimeMillis();
        if (!onHeap.containsKey(key)) {
            result = generateDataByKey(key);
        } else {
            byte[] bytes = onHeap.get(key);
            // decode
            final ByteBuf f = Unpooled.wrappedBuffer(bytes);
            try {
                result = (List<PQ>) coder.getValueDecoder().decode(f, null);
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

        imapTimer.record(System.currentTimeMillis() - start, TimeUnit.MILLISECONDS);

        return new ResponseEntity<>(result, HttpStatus.OK);
    }


    private List<PQ> generateDataByKey(final UUID key) {


        final IMap<UUID, byte[]> onHeap = instance.getMap("pq_on_heap");

        List<PQ> threeWeeks = new ArrayList<>();

        long startTs = System.currentTimeMillis();
        final Random r = new Random();
        final float max = 255;
        final float min = 100;
        IntStream.range(0, 6048).forEach(i -> {
            // load from db
            PQ pq = new PQ(new DeviceKey(key), new Timestamp(startTs + i * (1000 * 60 * 5)));

            pq.setVoltageA(min + r.nextFloat() * (max - min));
            pq.setVoltageB(min + r.nextFloat() * (max - min));
            pq.setVoltageC(min + r.nextFloat() * (max - min));

            pq.setActivePowerA(min + r.nextFloat() * (max - min));
            pq.setActivePowerB(min + r.nextFloat() * (max - min));
            pq.setActivePowerC(min + r.nextFloat() * (max - min));

            pq.setInactivePowerA(min + r.nextFloat() * (max - min));
            pq.setInactivePowerB(min + r.nextFloat() * (max - min));
            pq.setInactivePowerC(min + r.nextFloat() * (max - min));
            pq.setHistogram(new int[]{0, 1, 1, 2, 3, 4, 5, 6, 7, 8, 9, 0});
            threeWeeks.add(pq);
        });


        ByteBuf byteBuf = null;
        try {
            byteBuf = coder.getValueEncoder().encode(threeWeeks);
            byte[] bytes = new byte[byteBuf.readableBytes()];
            byteBuf.duplicate().readBytes(bytes);
            onHeap.put(key, bytes);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            if (byteBuf != null) {
                byteBuf.release();
            }
        }


        return threeWeeks;
    }


    private void generateData() {
        // get IMap
        final IMap<UUID, byte[]> onHeap = instance.getMap("pq_on_heap");

        // TODO:// pass DTOGenerator classloader into codec
        final LZ4Codec coder = new LZ4Codec(new Kryo5Codec());

        IntStream.range(0, 10000).forEach(w -> {
            //
            UUID k = UUID.randomUUID();

            List<PQ> threeWeeks = new ArrayList<>();
            IntStream.range(0, 6048).forEach(i -> {


                PQ pq = new PQ(new DeviceKey(UUID.randomUUID()), new Timestamp(System.currentTimeMillis()));

                pq.setVoltageA(100);
                pq.setVoltageB(100);
                pq.setVoltageC(100);

                pq.setActivePowerA(100);
                pq.setActivePowerB(100);
                pq.setActivePowerC(100);

                pq.setInactivePowerA(100);
                pq.setInactivePowerB(100);
                pq.setInactivePowerC(100);
                threeWeeks.add(pq);
            });

            ByteBuf byteBuf = null;
            try {
                byteBuf = coder.getValueEncoder().encode(threeWeeks);
                byte[] bytes = new byte[byteBuf.readableBytes()];
                byteBuf.duplicate().readBytes(bytes);
                onHeap.put(k, bytes);

                // try decode
//                byte[] l = onHeap.get(k);
//
//                final ByteBuf f = Unpooled.wrappedBuffer(l);
//
//                Object value = coder.getValueDecoder().decode(f, null);
//
//                System.out.println("~");


            } catch (IOException e) {
                throw new RuntimeException(e);
            } finally {
                if (byteBuf != null) {
                    byteBuf.release();
                }
            }


            if (w != 0 && w % 1000 == 0) {
                System.out.println("done with " + w + " devices");
            }
        });
    }


    @Override
    public void run(String... args) throws Exception {


    }
}
