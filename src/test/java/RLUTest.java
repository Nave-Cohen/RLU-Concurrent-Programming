import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

import java.util.concurrent.*;

public class RLUTest {

    @Test
    public void testBasicOperations() {
        RLU<Integer> rlu = new RLU<>();

        // Test write operation in the same thread
        assertTrue(rlu.write(1, 100), "Expected write(1, 100) to succeed");
        // Uncommitted write is visible to the writer thread
        assertEquals(100, rlu.read(1), "Uncommitted read(1) should return 100");

        // After commit, the value remains visible
        rlu.commit();
        assertEquals(100, rlu.read(1), "After commit, read(1) should return 100");

        // Test remove operation
        assertTrue(rlu.remove(1), "Expected remove(1) to succeed");
        rlu.commit();
        assertNull(rlu.read(1), "After remove commit, read(1) should return null");
    }

    @Test
    public void testThreadIsolation() throws InterruptedException, ExecutionException {
        RLU<Integer> rlu = new RLU<>();
        ExecutorService executor = Executors.newFixedThreadPool(2);

        // Writer thread: perform write
        Future<Boolean> writeFuture = executor.submit(() -> rlu.write(1, 100));
        assertTrue(writeFuture.get(), "Write should succeed in the writer thread");

        // Reader thread: try reading before commit (should see no value)
        Future<Integer> readBeforeCommit = executor.submit(() -> rlu.read(1));
        assertNull(readBeforeCommit.get(), "Uncommitted value should not be visible across threads");

        // Commit the update from a separate thread
        Future<Boolean> commitFuture = executor.submit(rlu::commit);
        assertTrue(commitFuture.get(), "Commit should succeed");

        // Reader thread: verify that the committed value is visible
        Future<Integer> readAfterCommit = executor.submit(() -> rlu.read(1));
        assertEquals(100, readAfterCommit.get(), "After commit, read(1) should return 100");

        executor.shutdown();
    }

    @Test
    public void testDiscardFunctionality() throws InterruptedException, ExecutionException {
        RLU<Integer> rlu = new RLU<>();
        ExecutorService executor = Executors.newSingleThreadExecutor();

        // Write and then discard the update in the same thread
        Future<Boolean> discardResult = executor.submit(() -> {
            rlu.write(1, 300);
            rlu.discard(1);
            return rlu.read(1) == null;
        });
        assertTrue(discardResult.get(), "After discard, read(1) should return null");

        executor.shutdown();
    }

    @Test
    public void testConcurrentModifications() throws InterruptedException, ExecutionException {
        RLU<Integer> rlu = new RLU<>();
        ExecutorService executor = Executors.newFixedThreadPool(4);

        // Thread 1 writes a value and delays its commit
        Future<Boolean> thread1Commit = executor.submit(() -> {
            rlu.write(1, 100);
            Thread.sleep(200); // simulate delay
            return rlu.commit();
        });

        // Thread 2 writes a new value and commits immediately
        Future<Boolean> thread2Commit = executor.submit(() -> {
            rlu.write(1, 200);
            return rlu.commit();
        });

        // Expect thread 1's commit to fail due to conflict
        assertFalse(thread1Commit.get(), "Thread 1 commit should fail because of concurrent modification");
        assertTrue(thread2Commit.get(), "Thread 2 commit should succeed");

        // Verify the final value is 200
        Future<Integer> finalValueFuture = executor.submit(() -> rlu.read(1));
        assertEquals(200, finalValueFuture.get(), "Final value should be 200");

        // Additional update: commit a new value (300)
        Future<Boolean> thread3Commit = executor.submit(() -> {
            rlu.write(1, 300);
            return rlu.commit();
        });
        assertTrue(thread3Commit.get(), "Thread 3 commit should succeed");

        Future<Integer> finalValueAfterThread3 = executor.submit(() -> rlu.read(1));
        assertEquals(300, finalValueAfterThread3.get(), "Final value should be 300 after thread 3 commit");

        executor.shutdown();
    }

    @Test
    public void testConcurrentWrites() throws InterruptedException {
        RLU<Integer> rlu = new RLU<>();
        ExecutorService executor = Executors.newFixedThreadPool(3);

        Runnable writer1 = () -> {
            rlu.write(1, 100);
            rlu.commit();
        };
        Runnable writer2 = () -> {
            rlu.write(1, 200);
            rlu.commit();
        };
        Runnable remover = () -> {
            rlu.remove(1);
            rlu.commit();
        };

        // Execute concurrent operations
        executor.execute(writer1);
        executor.execute(writer2);
        executor.execute(remover);

        executor.shutdown();
        assertTrue(executor.awaitTermination(5, TimeUnit.SECONDS), "Executor did not terminate in time");

        // Final value could be null (if remove was last), 100, or 200
        Integer finalValue = rlu.read(1);
        assertTrue(finalValue == null || finalValue == 100 || finalValue == 200,
                "Final value of key 1 should be null, 100, or 200, but was: " + finalValue);
    }
}
