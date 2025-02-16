import org.junit.jupiter.api.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class RLUCommitTest {
    private static final int NUM_THREADS = 10; // Number of concurrent writers
    private static final int NUM_ENTRIES = 1000; // Number of objects each thread modifies
    private RLU<Integer> rlu;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        rlu = new RLU<>();
        executor = Executors.newFixedThreadPool(NUM_THREADS);
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    @DisplayName("Test Concurrent Writes, Commits, and Removes")
    void testConcurrentWritesCommitsAndRemovals() throws InterruptedException {
        CountDownLatch writeLatch = new CountDownLatch(NUM_THREADS);

        // Step 1: Simulate Concurrent Writes
        for (int i = 0; i < NUM_THREADS; i++) {
            final int threadId = i;
            executor.submit(() -> {
                for (int key = 0; key < NUM_ENTRIES; key++) {
                    int value = (threadId + 1) * 100 + key; // Unique value per thread
                    rlu.write(key, value);
                }
                writeLatch.countDown();
            });
        }

        // Wait for all writes to finish
        writeLatch.await();

        // Step 2: Concurrent Commits
        CountDownLatch commitLatch = new CountDownLatch(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                rlu.commit();
                commitLatch.countDown();
            });
        }

        // Wait for all commits to finish
        commitLatch.await();

        // Step 3: Validate that all keys exist in MainLog after commit
        for (int key = 0; key < NUM_ENTRIES; key++) {
            Integer finalValue = rlu.read(key);
            assertNotNull(finalValue, "ERROR: Key " + key + " is missing in MainLog after commit!");
        }

        // Step 4: Simulate Concurrent Removals
        CountDownLatch removeLatch = new CountDownLatch(NUM_THREADS);
        for (int i = 0; i < NUM_THREADS; i++) {
            executor.submit(() -> {
                for (int key = 0; key < NUM_ENTRIES; key += 2) { // Remove only even keys
                    rlu.remove(key);
                    rlu.commit();
                }
                removeLatch.countDown();
            });
        }

        // Wait for all removals to finish
        removeLatch.await();

        // Step 6: Validate that removed keys are gone and others still exist

        for (int key = 0; key < NUM_ENTRIES; key++) {
            Integer finalValue = rlu.read(key);
            if (key % 2 == 0) {
                assertNull(finalValue, "ERROR: Key " + key + " was not removed properly!");
            } else {
                assertNotNull(finalValue, "ERROR: Key " + key + " should still exist but is missing!");
            }
        }
    }
}
