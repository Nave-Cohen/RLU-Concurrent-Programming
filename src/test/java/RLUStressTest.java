import org.junit.jupiter.api.Test;

import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class RLUStressTest {

    @Test
    public void testInsertAndRemoveOperations() throws InterruptedException {
        RLU<Integer> set = new RLU<>();
        int numThreads = 4;
        int numOps = 1000;
        int keyRange=20;
        Random rand = new Random();
        AtomicInteger writeCounter = new AtomicInteger(0);
        AtomicInteger readCounter = new AtomicInteger(0);
        AtomicInteger removeCounter = new AtomicInteger(0);
        ExecutorService executor = Executors.newFixedThreadPool(numThreads);

        for (int i = 0; i < numThreads; i++) {
            executor.execute(() -> {
                for (int j = 0; j < numOps; j++) {
                    int key = rand.nextInt(keyRange);
                    set.write(key, key);
                    set.commit();
                    writeCounter.incrementAndGet();

                    // Validate Insert (key should be found)
                    if (set.read(key) != null) {
                        readCounter.incrementAndGet();
                    }
                    set.remove(key);
                    set.commit();
                    if (set.read(key) == null) {
                        removeCounter.incrementAndGet();
                    }
                }
            });
        }
        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in expected time.");

        for (int i=numOps%100;i>=0;i--) {
            assertNull(set.read(i), "Key " + i + " should not be found after remove.");
        }

        System.out.println("\nStressTest completed:");
        System.out.println(" Thread number: "+ numThreads);
        System.out.println(" Key range: "+ keyRange);
        System.out.println(" Write Count: " + writeCounter.get()+", Success read Count: " + readCounter.get()+", Success remove Count: " + removeCounter.get());

        // Validate insert/remove consistency
        assertTrue(readCounter.get() > 0, "At least some `read()` calls should have succeeded.");
    }

}
