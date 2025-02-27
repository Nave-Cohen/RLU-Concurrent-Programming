public class MppRunner {

    public static void main(String[] args) {
        // Scenario 1: 1,000 buckets, 100 nodes per bucket, various update percentages
        runBenchmark(new int[]{2, 20, 40}, 1000, 100, 1000);

        // Scenario 2: 10,000 buckets, 1 node per bucket, 100% updates
        runBenchmark(new int[]{100}, 10000, 1, 1000);
    }

    private static void runBenchmark(int[] updatePercentages, int bucketCount, int nodes, int durationMillis) {
        for (int updatePercent : updatePercentages) {
            System.out.println("\n=== Running Benchmark for " + updatePercent
                    + "% Updates (" + bucketCount + " buckets, " + nodes + " nodes each) ===");

            int percentRead = 100 - updatePercent;
            int percentWrite = updatePercent/2;

            for (int numThreads = 1; numThreads <= 64; numThreads *= 2) {
                RLU<Integer> list = new RLU<>(bucketCount);
                RLUBenchmark.runTest(list, numThreads, percentRead, percentWrite, bucketCount, nodes, durationMillis);
            }
        }
    }
}
