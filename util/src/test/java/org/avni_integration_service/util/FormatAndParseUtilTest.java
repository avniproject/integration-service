package org.avni_integration_service.util;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class FormatAndParseUtilTest {
    // Avni's "...Z" strings round-trip exactly: read as the JVM's wall-clock, written back with a Z.
    @Test
    public void anAvniTimestampRoundTripsUnchanged() {
        for (String s : List.of("2026-10-07T08:45:12.345Z", "2026-01-01T00:00:00.000Z", "2026-12-31T23:59:59.999Z"))
            assertEquals(s, FormatAndParseUtil.toISODateTimeString(FormatAndParseUtil.fromAvniDateTime(s)));
    }

    // Two organisations' jobs parse and format at the same moment on scheduler threads.
    @Test
    public void manyThreadsParsingAndFormattingAtOnceGetTheRightAnswers() throws Exception {
        List<String> inputs = new ArrayList<>();
        for (int i = 0; i < 50; i++)
            inputs.add(String.format("20%02d-%02d-%02dT%02d:%02d:%02d.%03dZ", 10 + i % 20, 1 + i % 12, 1 + i % 28, i % 24, i % 60, (7 * i) % 60, (37 * i) % 1000));
        int threads = 16;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger wrong = new AtomicInteger();
        List<Future<?>> futures = new ArrayList<>();
        for (int t = 0; t < threads; t++) {
            futures.add(pool.submit(() -> {
                start.await();
                for (int round = 0; round < 400; round++) {
                    for (String input : inputs) {
                        try {
                            if (!input.equals(FormatAndParseUtil.toISODateTimeString(FormatAndParseUtil.fromAvniDateTime(input))))
                                wrong.incrementAndGet();
                        } catch (RuntimeException e) {
                            wrong.incrementAndGet();
                        }
                    }
                }
                return null;
            }));
        }
        start.countDown();
        for (Future<?> f : futures) f.get(60, TimeUnit.SECONDS);
        pool.shutdown();
        assertEquals(0, wrong.get(), "wrong or failed parses under concurrency");
    }
}
