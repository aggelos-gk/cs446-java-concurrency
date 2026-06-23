import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Supplier;

public final class QueueComparisonMain {
    private static final int WARMUP_OPERATIONS = 100_000;
    private static final int MEASURED_OPERATIONS = 1_000_000;
    private static final int THREAD_OPERATIONS = 100_000;
    private static final int[] THREAD_COUNTS = {1, 4, 8, 16, 32};

    private QueueComparisonMain() {
    }

    public static void main(String[] args) {
        QueueCase[] cases = {
                new QueueCase("lcrq", LinkedConcurrentRingQueue::new),
                new QueueCase("msqueue", MSLinkedConcurrentQueue::new),
                new QueueCase("official", ConcurrentLinkedQueue::new)
        };

        String expectedOutput = null;
        boolean sameFunctionalOutput = true;

        System.out.println("=== Functional outputs ===");
        for (QueueCase queueCase : cases) {
            String output = QueueDemoSupport.functionalOutput(queueCase.create());
            if (expectedOutput == null) {
                expectedOutput = output;
            } else {
                sameFunctionalOutput &= expectedOutput.equals(output);
            }

            System.out.println("=== " + queueCase.name + " functional output ===");
            System.out.print(output);
        }
        System.out.println("same functional output across all queues: " + sameFunctionalOutput);

        System.out.println("=== Sequential timings ===");
        Timing[] sequentialTimings = new Timing[cases.length];
        for (int i = 0; i < cases.length; i++) {
            QueueDemoSupport.timeOfferPollNanos(cases[i].create(), WARMUP_OPERATIONS);
            long nanos = QueueDemoSupport.timeOfferPollNanos(cases[i].create(), MEASURED_OPERATIONS);
            sequentialTimings[i] = new Timing(cases[i].name, nanos);
            System.out.printf("%s time for %,d sequential offer+poll pairs: %.3f ms%n",
                    cases[i].name, MEASURED_OPERATIONS, QueueDemoSupport.millis(nanos));
        }
        printFastest(sequentialTimings);

        System.out.println("=== Threaded offer/poll tests ===");
        for (int threads : THREAD_COUNTS) {
            QueueDemoSupport.ConcurrentResult expected = null;
            boolean sameThreadedResult = true;
            Timing[] timings = new Timing[cases.length];

            for (int i = 0; i < cases.length; i++) {
                QueueDemoSupport.ConcurrentResult result = QueueDemoSupport.runConcurrentOfferPoll(
                        cases[i].create(), threads, THREAD_OPERATIONS);
                timings[i] = new Timing(cases[i].name, result.nanos);

                if (expected == null) {
                    expected = result;
                } else {
                    sameThreadedResult &= expected.comparableSummary().equals(result.comparableSummary());
                }

                System.out.printf("threads=%d %-8s %s, passed=%s, time=%.3f ms%n",
                        threads, cases[i].name, result.comparableSummary(), result.passed(),
                        QueueDemoSupport.millis(result.nanos));
            }

            System.out.printf("threads=%d same threaded result across all queues: %s%n", threads, sameThreadedResult);
            printFastest(timings);
        }
    }

    private static void printFastest(Timing[] timings) {
        Timing fastest = timings[0];
        for (Timing timing : timings) {
            if (timing.nanos < fastest.nanos) {
                fastest = timing;
            }
        }

        System.out.printf("fastest: %s", fastest.name);
        for (Timing timing : timings) {
            if (timing != fastest) {
                System.out.printf(", %.2fx faster than %s", (double) timing.nanos / fastest.nanos, timing.name);
            }
        }
        System.out.println();
    }

    private static final class QueueCase {
        final String name;
        final Supplier<Queue<Integer>> supplier;

        QueueCase(String name, Supplier<Queue<Integer>> supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        Queue<Integer> create() {
            return supplier.get();
        }
    }

    private static final class Timing {
        final String name;
        final long nanos;

        Timing(String name, long nanos) {
            this.name = name;
            this.nanos = nanos;
        }
    }
}
