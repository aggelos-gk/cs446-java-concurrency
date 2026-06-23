import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Queue;
import java.util.Spliterator;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.CyclicBarrier;
import java.util.function.Supplier;
import java.util.stream.Stream;

final class QueueTestSupport {
    static final int[] THREAD_COUNTS = {1, 4, 8, 16, 32};

    private QueueTestSupport() {
    }

    static Stream<QueueCase> customQueueCases() {
        return Stream.of(
                new QueueCase("lcrq", () -> new java.util.concurrent.LinkedConcurrentRingQueue<>()),
                new QueueCase("msqueue", () -> new java.util.concurrent.MSConcurrentLinkedQueue<>())
        );
    }

    static Stream<QueueCase> allQueueCases() {
        return Stream.of(
                new QueueCase("lcrq", () -> new java.util.concurrent.LinkedConcurrentRingQueue<>()),
                new QueueCase("msqueue", () -> new java.util.concurrent.MSConcurrentLinkedQueue<>()),
                new QueueCase("official", ConcurrentLinkedQueue::new)
        );
    }

    static List<Integer> integerValues() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            values.add((i % 2 == 0) ? i * 7 : -i * 3);
        }
        return values;
    }

    static List<String> stringValues() {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            values.add("value-" + i + "-" + (char) ('a' + (i % 26)));
        }
        return values;
    }

    static List<Object> mixedValues() {
        return Arrays.asList(
                "one", 2, "three", 4, "five", 6L, "seven", 8.5,
                "nine", -10, "eleven", 12L, "thirteen", 14.25,
                "fifteen", 16, "seventeen", -18L, "nineteen", 20.75);
    }

    static List<Object> listFromIterator(Iterable<?> iterable) {
        List<Object> out = new ArrayList<>();
        for (Object value : iterable) {
            out.add(value);
        }
        return out;
    }

    static List<Object> listFromSpliterator(Spliterator<?> spliterator) {
        List<Object> out = new ArrayList<>();
        spliterator.forEachRemaining(out::add);
        return out;
    }

    static List<Object> drain(Queue<?> queue) {
        List<Object> out = new ArrayList<>();
        Object value;
        while ((value = queue.poll()) != null) {
            out.add(value);
        }
        return out;
    }

    static int countIterator(java.util.Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    static int countSpliterator(Spliterator<?> spliterator) {
        final int[] count = {0};
        spliterator.forEachRemaining(value -> count[0]++);
        return count[0];
    }

    static List<String> sortedStrings(Object[] values) {
        List<String> out = new ArrayList<>();
        for (Object value : values) {
            out.add(String.valueOf(value));
        }
        Collections.sort(out);
        return out;
    }

    static ThreadedResult runThreaded(Queue<Object> queue, int threads, int roundsPerThread) throws Exception {
        Thread[] workers = new Thread[threads];
        CyclicBarrier start = new CyclicBarrier(threads + 1);

        for (int threadId = 0; threadId < threads; threadId++) {
            final int id = threadId;
            workers[threadId] = new Thread(() -> {
                await(start);
                for (int round = 0; round < roundsPerThread; round++) {
                    int base = id * roundsPerThread + round;
                    queue.add("s" + base);
                    queue.offer(base);
                    queue.addAll(Arrays.asList("batch" + base, -base));
                }
            }, "queue-api-test-" + threadId);
            workers[threadId].start();
        }

        await(start);
        joinAll(workers);

        int expectedBeforeRemove = threads * roundsPerThread * 4;
        boolean containsFirstString = queue.contains("s0");
        boolean containsLastInt = queue.contains(threads * roundsPerThread - 1);
        boolean removeString = queue.remove("batch0");
        boolean removeInt = queue.remove(-(threads * roundsPerThread - 1));
        boolean removeMissing = queue.remove("missing-value");
        int sizeAfterRemove = queue.size();
        int iteratorCount = countIterator(queue.iterator());
        int spliteratorCount = countSpliterator(queue.spliterator());
        int toArrayLength = queue.toArray().length;
        int typedToArrayLength = queue.toArray(new Object[0]).length;
        int expectedAfterRemove = expectedBeforeRemove - 2;

        List<String> sortedRemaining = sortedStrings(queue.toArray());
        List<Object> drained = drain(queue);
        List<String> sortedDrained = sortedStrings(drained.toArray());

        return new ThreadedResult(expectedBeforeRemove, expectedAfterRemove, containsFirstString, containsLastInt,
                removeString, removeInt, removeMissing, sizeAfterRemove, iteratorCount, spliteratorCount,
                toArrayLength, typedToArrayLength, sortedRemaining, sortedDrained, queue.isEmpty(), queue.poll());
    }

    private static void await(CyclicBarrier barrier) {
        try {
            barrier.await();
        } catch (Exception e) {
            throw new IllegalStateException(e);
        }
    }

    private static void joinAll(Thread[] threads) throws Exception {
        for (Thread thread : threads) {
            thread.join();
        }
    }

    static void assertTrue(boolean condition) {
        assertTrue(condition, "expected condition to be true");
    }

    static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    static void assertFalse(boolean condition) {
        assertFalse(condition, "expected condition to be false");
    }

    static void assertFalse(boolean condition, String message) {
        if (condition) {
            throw new AssertionError(message);
        }
    }

    static void assertEquals(Object expected, Object actual) {
        assertEquals(expected, actual, "expected=" + expected + ", actual=" + actual);
    }

    static void assertEquals(Object expected, Object actual, String message) {
        if (!Objects.equals(expected, actual)) {
            throw new AssertionError(message + " (expected=" + expected + ", actual=" + actual + ")");
        }
    }

    static void assertArrayEquals(Object[] expected, Object[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new AssertionError("array mismatch expected=" + Arrays.toString(expected)
                    + ", actual=" + Arrays.toString(actual));
        }
    }

    static <T extends Throwable> void assertThrows(Class<T> expectedType, ThrowingRunnable action) {
        try {
            action.run();
        } catch (Throwable t) {
            if (expectedType.isInstance(t)) {
                return;
            }
            throw new AssertionError("expected exception " + expectedType.getName()
                    + " but got " + t.getClass().getName(), t);
        }
        throw new AssertionError("expected exception " + expectedType.getName());
    }

    @FunctionalInterface
    interface ThrowingRunnable {
        void run() throws Exception;
    }

    static final class QueueCase {
        final String name;
        final Supplier<Queue<Object>> supplier;

        QueueCase(String name, Supplier<Queue<Object>> supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        Queue<Object> create() {
            return supplier.get();
        }

        @Override
        public String toString() {
            return name;
        }
    }

    static final class ThreadedResult {
        final int expectedBeforeRemove;
        final int expectedAfterRemove;
        final boolean containsFirstString;
        final boolean containsLastInt;
        final boolean removeString;
        final boolean removeInt;
        final boolean removeMissing;
        final int sizeAfterRemove;
        final int iteratorCount;
        final int spliteratorCount;
        final int toArrayLength;
        final int typedToArrayLength;
        final List<String> sortedRemaining;
        final List<String> sortedDrained;
        final boolean emptyAfterDrain;
        final Object pollAfterDrain;

        ThreadedResult(int expectedBeforeRemove, int expectedAfterRemove, boolean containsFirstString,
                boolean containsLastInt, boolean removeString, boolean removeInt, boolean removeMissing,
                int sizeAfterRemove, int iteratorCount, int spliteratorCount, int toArrayLength,
                int typedToArrayLength, List<String> sortedRemaining, List<String> sortedDrained,
                boolean emptyAfterDrain, Object pollAfterDrain) {
            this.expectedBeforeRemove = expectedBeforeRemove;
            this.expectedAfterRemove = expectedAfterRemove;
            this.containsFirstString = containsFirstString;
            this.containsLastInt = containsLastInt;
            this.removeString = removeString;
            this.removeInt = removeInt;
            this.removeMissing = removeMissing;
            this.sizeAfterRemove = sizeAfterRemove;
            this.iteratorCount = iteratorCount;
            this.spliteratorCount = spliteratorCount;
            this.toArrayLength = toArrayLength;
            this.typedToArrayLength = typedToArrayLength;
            this.sortedRemaining = sortedRemaining;
            this.sortedDrained = sortedDrained;
            this.emptyAfterDrain = emptyAfterDrain;
            this.pollAfterDrain = pollAfterDrain;
        }
    }
}
