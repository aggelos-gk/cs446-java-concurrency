package benchmarks;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedConcurrentRingQueue;
import java.util.concurrent.MSConcurrentLinkedQueue;
import java.util.concurrent.atomic.AtomicInteger;

import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

public final class QueueBenchmarkSupport {
    private QueueBenchmarkSupport() {
    }

    static Queue<Integer> newQueue(String implementation) {
        return switch (implementation) {
            case "jdk" -> new ConcurrentLinkedQueue<>();
            case "ms" -> new MSConcurrentLinkedQueue<>();
            case "lcrq" -> new LinkedConcurrentRingQueue<>();
            default -> throw new IllegalArgumentException("Unknown implementation: " + implementation);
        };
    }

    @State(Scope.Benchmark)
    public static class SharedQueueState {
        @Param({"jdk", "ms", "lcrq"})
        public String implementation;

        @Param({"1024"})
        public int prefill;

        public Queue<Integer> queue;
        public AtomicInteger sequence;

        @Setup(Level.Iteration)
        public void setup() {
            queue = newQueue(implementation);
            sequence = new AtomicInteger();

            for (int i = 0; i < prefill; i++) {
                queue.offer(sequence.getAndIncrement());
            }
        }
    }

    @State(Scope.Group)
    public static class GroupQueueState {
        @Param({"jdk", "ms", "lcrq"})
        public String implementation;

        @Param({"4096"})
        public int prefill;

        public Queue<Integer> queue;
        public AtomicInteger sequence;

        @Setup(Level.Iteration)
        public void setup() {
            queue = newQueue(implementation);
            sequence = new AtomicInteger();

            for (int i = 0; i < prefill; i++) {
                queue.offer(sequence.getAndIncrement());
            }
        }
    }
}
