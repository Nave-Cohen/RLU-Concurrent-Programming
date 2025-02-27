import java.util.Random;

class RLUBenchmark extends Thread {
    private final RLU<Integer> list;
    private final int percentRead;
    private final int percentWrite;
    private final int bucketCount;
    private final int nodes;
    private int numOps = 0;

    public RLUBenchmark(RLU<Integer> list, int percentRead, int percentWrite, int bucketCount, int nodes) {
        this.list = list;
        this.percentRead = percentRead;
        this.percentWrite = percentWrite;
        this.bucketCount = bucketCount;
        this.nodes = nodes;
    }

    @Override
    public void run() {
        Random rand = new Random();
        final int perRead = this.percentRead;

        while (!Thread.currentThread().isInterrupted()) {
            int op = rand.nextInt(100);
            // Spread nodes evenly in buckets
            int bucketIndex = rand.nextInt(bucketCount);
            int keyWithinBucket = rand.nextInt(nodes);
            int key = bucketIndex * nodes + keyWithinBucket;

            if (op < perRead) {
                list.read(key);
            } else if (op < perRead + this.percentWrite) {
                list.write(key, key);
                list.commit();

            } else {
                list.remove(key);
                list.commit();
            }
            ++this.numOps;
        }
    }

    /**
     * Runs a throughput test for the RLU.
     *
     * @param list the RLU instance to test
     * @param numThreads number of concurrent threads
     * @param perRead percentage of read(x) operations
     * @param perWrite percentage of write(x) operations
     * @param bucketCount count of hash table buckets
     * @param nodes count of nodes for each bucket
     * @param ms test duration in milliseconds
     */
    public static void runTest(RLU<Integer> list, int numThreads, int perRead, int perWrite, int bucketCount, int nodes, int ms) {
        final int perRemove = 100 - perWrite - perRead;
        RLUBenchmark[] threads = new RLUBenchmark[numThreads];

        for (int i = 0; i < numThreads; ++i)
            threads[i] = new RLUBenchmark(list, perRead, perWrite, bucketCount, nodes);

        // Start threads
        for (int i = 0; i < numThreads; ++i) {
            threads[i].start();
        }

        // Wait for the test duration
        try {
            Thread.sleep(ms);
        } catch (InterruptedException ignored) {}

        // Stop threads
        for (Thread t : threads)
            t.interrupt();

        // Wait for threads to actually finish
        for (Thread t : threads) {
            try {
                t.join();
            } catch (InterruptedException ignored) {}
        }

        // Compute throughput
        long totalOps = 0;
        for (RLUBenchmark t : threads) {
            totalOps += t.numOps;
        }
        double throughput = totalOps / (1000.0 * ms);
        System.out.println("["+numThreads+"]" + " (" + perRemove + "% remove, " + perWrite + "% write, " + perRead + "% read): " + (long)throughput + " Operations/µ s");
    }
}
