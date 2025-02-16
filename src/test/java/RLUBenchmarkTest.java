import org.junit.jupiter.api.Test;

import java.text.NumberFormat;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RLUBenchmarkTest {

    static class RLUSetBenchmark implements Runnable {
        private final RLU<Integer> list;
        private final int keyRange;
        private final int percentRead;
        private final int percentWrite;
        private final int durationMillis;
        private final AtomicInteger numOps;

        public RLUSetBenchmark(RLU<Integer> list, int keyRange, int percentRead, int percentWrite,int durationMillis, AtomicInteger numOps) {
            this.list = list;
            this.keyRange = keyRange;
            this.percentRead = percentRead;
            this.percentWrite = percentWrite;
            this.durationMillis = durationMillis;
            this.numOps = numOps;
        }

        @Override
        public void run() {
            Random rand = new Random();
            long endTime = System.currentTimeMillis() + durationMillis;

            while (System.currentTimeMillis() < endTime) {
                int op = rand.nextInt(100);
                int key = rand.nextInt(this.keyRange);

                if (op < percentRead) {
                    list.read(key);
                } else if (op < percentRead + percentWrite) {
                    list.write(key, key);
                    list.commit();
                } else {
                    list.remove(key);
                    list.commit();
                }

                numOps.incrementAndGet();
            }
        }
    }

    @Test
    public void testRLUSetBenchmark() throws InterruptedException {
        int numThreads = 4;
        int keyRange = 10000;
        int percentRead = 10;
        int percentWrite = 40;
        int percentRemove = 100-percentWrite-percentRead;
        int durationMillis = 5000; // 5 seconds

        RLU<Integer> rlu = new RLU<>();
        AtomicInteger totalOps = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.execute(new RLUSetBenchmark(rlu, keyRange, percentRead, percentWrite, durationMillis, totalOps));
        }

        executor.shutdown();
        assertTrue(executor.awaitTermination(durationMillis + 1000, TimeUnit.MILLISECONDS), "Benchmark did not complete in expected time.");

        double opsPerSec = totalOps.get() / (durationMillis / 1000.0);
        NumberFormat formatter = NumberFormat.getInstance();

        System.out.println("\nBenchmark completed:");
        System.out.println(" Thread number: " + numThreads);
        System.out.println(" Test duration: " + formatter.format(durationMillis) + " milliseconds");
        System.out.println(" Ops Split: Read: " + percentRead + "%, Write: " + percentWrite + "%, Remove: " + percentRemove + "%");
        System.out.println(" Total operations: " + formatter.format(totalOps.get()));
        System.out.println(" Throughput: " + formatter.format(opsPerSec) + " ops/sec");

    }
}
