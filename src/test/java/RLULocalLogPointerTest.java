import org.junit.jupiter.api.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;

class RLULocalLogPointerTest {
    private RLU<Integer> rlu;
    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        rlu = new RLU<>();
        executor = Executors.newFixedThreadPool(2); // Two threads: one writer, one reader
    }

    @AfterEach
    void tearDown() {
        executor.shutdown();
    }

    @Test
    @DisplayName("Test If MainLog Correctly References LocalLog for Other Threads")
    void testMainLogPointerVisibilityAcrossThreads() throws InterruptedException, ExecutionException {
        int key = 100;

        // Thread 1: Write and Commit
        Future<Boolean> writerThread = executor.submit(() -> {
            rlu.write(key, 42);  // Write value in LocalLog
            return rlu.commit();        // Commit, making MainLog reference LocalLog's committed value
        });

        writerThread.get(); // Ensure writer thread completes

        // Thread 2: Read from MainLog (should see the committed value)
        Future<Integer> readerThread = executor.submit(() -> {
            return rlu.read(key); // Should read 42 if MainLog correctly references LocalLog
        });

        assertEquals(42, readerThread.get(), "ERROR: Thread 2 should see the committed value 42!");

        // Thread 1: Modify LocalLog again (but DO NOT commit)
        writerThread = executor.submit(() -> {
            return rlu.write(key, 99); // Modify LocalLog but do NOT commit
        });

        writerThread.get();

        // Thread 2: Read again (should still see the old committed value 42, NOT 99)
        readerThread = executor.submit(() -> {
            return rlu.read(key); // Should still be 42 because 99 is uncommitted
        });

        assertEquals(42, readerThread.get(), "ERROR: Thread 2 should still see the committed value 42, NOT 99!");

        // Thread 1: Commit again
        writerThread = executor.submit(() -> {
            return rlu.commit(); // Now 99 should be visible in MainLog
        });

        writerThread.get();

        // Thread 2: Read after commit (should now see 99)
        readerThread = executor.submit(() -> {
            return rlu.read(key); // Should now be 99 after commit
        });

        assertEquals(99, readerThread.get(), "ERROR: Thread 2 should see the updated committed value 99!");
    }
}
