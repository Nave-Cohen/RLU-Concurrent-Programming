public class MppRunner {
        public static void main(String[] args) {
            int keyRange = 1000;
            int[] updatePercentages = {2, 20, 40};
            int durationMillis = 1000; // 1 seconds

            for (int updatePercent : updatePercentages) {
                System.out.println("\n=== Running Benchmark for " + updatePercent + "% Updates ===");

                for (int numThreads = 1; numThreads <= 16; numThreads *= 2) {
                    RLU<Integer> list = new RLU<>(false);
                    int percentRead = 100 - updatePercent;
                    int percentWrite = updatePercent / 2;

                RLUBenchmark.runTest(list, numThreads, keyRange, percentRead, percentWrite, durationMillis);
            }}

        }
}
