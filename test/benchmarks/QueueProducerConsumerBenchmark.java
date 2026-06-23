package benchmarks;

import java.util.concurrent.TimeUnit;

import org.openjdk.jmh.annotations.*;

@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 1, timeUnit = TimeUnit.SECONDS)
@Fork(1)
public class QueueProducerConsumerBenchmark {

    @Group("pc_1p_1c")
    @GroupThreads(1)
    @Benchmark
    public void producer1p1c(QueueBenchmarkSupport.GroupQueueState state) {
        state.queue.offer(state.sequence.getAndIncrement());
    }

    @Group("pc_1p_1c")
    @GroupThreads(1)
    @Benchmark
    public int consumer1p1c(QueueBenchmarkSupport.GroupQueueState state) {
        Integer value = state.queue.poll();
        return value == null ? -1 : value;
    }

    @Group("pc_2p_2c")
    @GroupThreads(2)
    @Benchmark
    public void producer2p2c(QueueBenchmarkSupport.GroupQueueState state) {
        state.queue.offer(state.sequence.getAndIncrement());
    }

    @Group("pc_2p_2c")
    @GroupThreads(2)
    @Benchmark
    public int consumer2p2c(QueueBenchmarkSupport.GroupQueueState state) {
        Integer value = state.queue.poll();
        return value == null ? -1 : value;
    }

    @Group("pc_4p_4c")
    @GroupThreads(4)
    @Benchmark
    public void producer4p4c(QueueBenchmarkSupport.GroupQueueState state) {
        state.queue.offer(state.sequence.getAndIncrement());
    }

    @Group("pc_4p_4c")
    @GroupThreads(4)
    @Benchmark
    public int consumer4p4c(QueueBenchmarkSupport.GroupQueueState state) {
        Integer value = state.queue.poll();
        return value == null ? -1 : value;
    }
}
