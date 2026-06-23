import java.util.ArrayList;
import java.util.Queue;
import java.util.concurrent.BrokenBarrierException;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

final class QueueDemoSupport {
    private QueueDemoSupport() {
    }

    static String functionalOutput(Queue<Integer> queue) {
        StringBuilder out = new StringBuilder();

        line(out, "offer(10)", queue.offer(10));
        line(out, "offer(20)", queue.offer(20));
        line(out, "offer(30)", queue.offer(30));
        line(out, "peek()", queue.peek());
        line(out, "poll()", queue.poll());
        line(out, "offer(40)", queue.offer(40));
        line(out, "poll()", queue.poll());
        line(out, "poll()", queue.poll());
        line(out, "peek()", queue.peek());
        line(out, "poll()", queue.poll());
        line(out, "poll()", queue.poll());
        line(out, "isEmpty()", queue.isEmpty());
        line(out, "offer(50)", queue.offer(50));
        line(out, "offer(60)", queue.offer(60));
        line(out, "size()", queue.size());
        line(out, "iterator()", new ArrayList<>(queue));
        line(out, "poll()", queue.poll());
        line(out, "poll()", queue.poll());
        line(out, "isEmpty()", queue.isEmpty());

        return out.toString();
    }

    static long timeOfferPollNanos(Queue<Integer> queue, int operations) {
        long start = System.nanoTime();

        for (int i = 0; i < operations; i++) {
            queue.offer(i);
        }

        long checksum = 0L;
        for (int i = 0; i < operations; i++) {
            Integer value = queue.poll();
            if (value == null) {
                throw new IllegalStateException("poll returned null at i=" + i);
            }
            checksum += value;
        }

        long elapsed = System.nanoTime() - start;
        long expected = checksumFor(1, operations);
        if (checksum != expected) {
            throw new IllegalStateException("bad checksum: " + checksum + ", expected " + expected);
        }

        return elapsed;
    }

    static ConcurrentResult runConcurrentOfferPoll(Queue<Integer> queue, int threads, int operationsPerThread) {
        AtomicLong checksum = new AtomicLong();
        AtomicInteger nullPolls = new AtomicInteger();
        AtomicInteger polled = new AtomicInteger();

        Thread[] workers = new Thread[threads];
        CyclicBarrier offerStart = new CyclicBarrier(threads + 1);

        long start = System.nanoTime();

        for (int threadId = 0; threadId < threads; threadId++) {
            final int id = threadId;
            workers[threadId] = new Thread(() -> {
                await(offerStart);

                int base = id * operationsPerThread;
                for (int i = 0; i < operationsPerThread; i++) {
                    queue.offer(base + i);
                }

            }, "queue-test-" + threads + "-" + threadId);
            workers[threadId].start();
        }

        await(offerStart);
        joinAll(workers);

        Thread[] pollers = new Thread[threads];
        CyclicBarrier pollOnlyStart = new CyclicBarrier(threads + 1);
        for (int threadId = 0; threadId < threads; threadId++) {
            pollers[threadId] = new Thread(() -> {
                await(pollOnlyStart);

                for (int i = 0; i < operationsPerThread; i++) {
                    Integer value = queue.poll();
                    if (value == null) {
                        nullPolls.incrementAndGet();
                    } else {
                        checksum.addAndGet(value);
                        polled.incrementAndGet();
                    }
                }
            }, "queue-poll-test-" + threads + "-" + threadId);
            pollers[threadId].start();
        }

        await(pollOnlyStart);
        joinAll(pollers);

        long elapsed = System.nanoTime() - start;
        int expectedCount = threads * operationsPerThread;
        long expectedChecksum = checksumFor(threads, operationsPerThread);
        int remaining = queue.size();

        return new ConcurrentResult(threads, operationsPerThread, expectedCount, polled.get(), nullPolls.get(), remaining,
                expectedChecksum, checksum.get(), elapsed);
    }

    static double millis(long nanos) {
        return nanos / 1_000_000.0;
    }

    private static long checksumFor(int threads, int operationsPerThread) {
        long total = (long) threads * operationsPerThread;
        return total * (total - 1) / 2;
    }

    private static void line(StringBuilder out, String operation, Object result) {
        out.append(operation).append(" -> ").append(result).append('\n');
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (BrokenBarrierException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void joinAll(Thread[] workers) {
        for (Thread worker : workers) {
            try {
                worker.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IllegalStateException(e);
            }
        }
    }

    static final class ConcurrentResult {
        final int threads;
        final int operationsPerThread;
        final int expectedCount;
        final int polledCount;
        final int nullPolls;
        final int remaining;
        final long expectedChecksum;
        final long checksum;
        final long nanos;

        ConcurrentResult(int threads, int operationsPerThread, int expectedCount, int polledCount, int nullPolls,
                int remaining, long expectedChecksum, long checksum, long nanos) {
            this.threads = threads;
            this.operationsPerThread = operationsPerThread;
            this.expectedCount = expectedCount;
            this.polledCount = polledCount;
            this.nullPolls = nullPolls;
            this.remaining = remaining;
            this.expectedChecksum = expectedChecksum;
            this.checksum = checksum;
            this.nanos = nanos;
        }

        boolean passed() {
            return polledCount == expectedCount
                    && nullPolls == 0
                    && remaining == 0
                    && checksum == expectedChecksum;
        }

        String comparableSummary() {
            return "threads=" + threads
                    + ", operationsPerThread=" + operationsPerThread
                    + ", expectedCount=" + expectedCount
                    + ", polledCount=" + polledCount
                    + ", nullPolls=" + nullPolls
                    + ", remaining=" + remaining
                    + ", checksum=" + checksum;
        }
    }
}
