import java.util.Random;

class RLUBenchmark extends Thread {
    private final RLU<Integer> list;
    private final int keyRange;
    private final int percentRead;
    private final int percentWrite;
    private int numOps = 0;

    public RLUBenchmark(RLU<Integer> list, int keyRange, int percentRead, int percentWrite) {
        this.list = list;
        this.keyRange = keyRange;
        this.percentRead = percentRead;
        this.percentWrite = percentWrite;
    }

    @Override
    public void run() {
        Random rand = new Random();
        final int perRead = this.percentRead;

        while (!Thread.currentThread().isInterrupted()) {
            int op = rand.nextInt(100);
            int key = rand.nextInt(this.keyRange);

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
     * @param keyRange range of keys to test [0, keyRange)
     * @param perRead percentage of read(x) operations
     * @param perWrite percentage of write(x) operations
     * @param ms test duration in milliseconds
     */
    public static void runTest(RLU<Integer> list, int numThreads, int keyRange, int perRead, int perWrite, int ms) {
        // Create threads
        final int perRemove = 100 - perWrite - perRead; // Calculate remove percentage
        RLUBenchmark[] threads = new RLUBenchmark[numThreads];

        for (int i = 0; i < numThreads; ++i)
            threads[i] = new RLUBenchmark(list, keyRange, perRead, perWrite);

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
        System.out.println(numThreads + "\t[" + keyRange +
                "](" + perRemove + "% remove, " + perWrite + "% write, " + perRead + "% read): " + (long)throughput + " Operations/µ s");
    }
}
