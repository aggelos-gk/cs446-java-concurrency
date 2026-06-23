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

public final class ConcurrentQueueApiComparisonMain {
    private static final int[] THREAD_COUNTS = {1, 4, 8, 16, 32};
    private static final int ROUNDS_PER_THREAD = 1_000;
    private static final int ENQUEUES_PER_ROUND = 4;

    private ConcurrentQueueApiComparisonMain() {
    }

    public static void main(String[] args) throws Exception {
        runSequentialApiScenario("integers", integerValues());
        runSequentialApiScenario("strings", stringValues());
        runMixedObjectScenario();
        runExceptionScenario();
        runThreadedScenarios();
    }

    private static <E> void runSequentialApiScenario(String label, List<E> values) {
        List<NamedQueue<E>> queues = namedQueues();
        List<String> names = namesOf(queues);
        List<String> lines = new ArrayList<>();

        compare(lines, names, "isEmpty initial", apply(queues, q -> q.isEmpty()));
        compare(lines, names, "add first", apply(queues, q -> q.add(values.get(0))));
        compare(lines, names, "offer second", apply(queues, q -> q.offer(values.get(1))));
        compare(lines, names, "addAll rest count=" + (values.size() - 2), apply(queues, q -> q.addAll(values.subList(2, values.size()))));
        compare(lines, names, "peek", apply(queues, Queue::peek));
        compare(lines, names, "contains first", apply(queues, q -> q.contains(values.get(0))));
        compare(lines, names, "contains middle", apply(queues, q -> q.contains(values.get(values.size() / 2))));
        compare(lines, names, "contains missing", apply(queues, q -> q.contains(missingValue())));
        compare(lines, names, "remove third", apply(queues, q -> q.remove(values.get(2))));
        compare(lines, names, "remove middle", apply(queues, q -> q.remove(values.get(values.size() / 2))));
        compare(lines, names, "remove missing", apply(queues, q -> q.remove(missingValue())));
        compare(lines, names, "size after removes", apply(queues, Queue::size));
        compare(lines, names, "iterator exact order", apply(queues, ConcurrentQueueApiComparisonMain::listFromIterator));
        compare(lines, names, "spliterator exact order", apply(queues, q -> listFromSpliterator(q.spliterator())));
        compare(lines, names, "toArray exact order", apply(queues, q -> Arrays.asList(q.toArray())));
        compare(lines, names, "toArray typed small", apply(queues, q -> Arrays.asList(q.toArray(new Object[0]))));
        compare(lines, names, "toArray typed large", apply(queues, q -> Arrays.asList(q.toArray(new Object[values.size() + 5]))));
        compare(lines, names, "poll 1", apply(queues, Queue::poll));
        compare(lines, names, "poll 2", apply(queues, Queue::poll));
        compare(lines, names, "poll 3", apply(queues, Queue::poll));
        compare(lines, names, "peek after polls", apply(queues, Queue::peek));
        compare(lines, names, "drain exact order", apply(queues, ConcurrentQueueApiComparisonMain::drain));
        compare(lines, names, "poll empty", apply(queues, Queue::poll));
        compare(lines, names, "isEmpty final", apply(queues, Queue::isEmpty));

        printSection("Sequential API scenario: " + label + " values=" + values.size(), lines);
    }

    private static void runMixedObjectScenario() {
        List<NamedQueue<Object>> queues = namedQueues();
        List<String> names = namesOf(queues);
        List<Object> mixed = Arrays.asList(
                "one", 2, "three", 4, "five", 6L, "seven", 8.5,
                "nine", -10, "eleven", 12L, "thirteen", 14.25,
                "fifteen", 16, "seventeen", -18L, "nineteen", 20.75);
        List<String> lines = new ArrayList<>();

        compare(lines, names, "add string", apply(queues, q -> q.add("zero")));
        compare(lines, names, "addAll mixed count=" + mixed.size(), apply(queues, q -> q.addAll(mixed)));
        compare(lines, names, "offer tail string", apply(queues, q -> q.offer("tail")));
        compare(lines, names, "contains string", apply(queues, q -> q.contains("three")));
        compare(lines, names, "contains integer", apply(queues, q -> q.contains(4)));
        compare(lines, names, "contains double", apply(queues, q -> q.contains(20.75)));
        compare(lines, names, "remove long", apply(queues, q -> q.remove(6L)));
        compare(lines, names, "remove negative long", apply(queues, q -> q.remove(-18L)));
        compare(lines, names, "iterator mixed exact order", apply(queues, ConcurrentQueueApiComparisonMain::listFromIterator));
        compare(lines, names, "toArray mixed exact order", apply(queues, q -> Arrays.asList(q.toArray())));
        compare(lines, names, "drain mixed exact order", apply(queues, ConcurrentQueueApiComparisonMain::drain));
        compare(lines, names, "empty mixed", apply(queues, Queue::isEmpty));

        printSection("Mixed Object API scenario values=" + (mixed.size() + 2), lines);
    }

    private static void runExceptionScenario() {
        List<NamedQueue<Object>> queues = namedQueues();
        List<String> names = namesOf(queues);
        List<String> lines = new ArrayList<>();

        compare(lines, names, "offer null exception", apply(queues, q -> exceptionName(() -> q.offer(null))));
        compare(lines, names, "add null exception", apply(queues, q -> exceptionName(() -> q.add(null))));
        compare(lines, names, "addAll self exception", apply(queues, q -> exceptionName(() -> q.addAll(q))));
        compare(lines, names, "contains null", apply(queues, q -> q.contains(null)));
        compare(lines, names, "remove null", apply(queues, q -> q.remove(null)));

        printSection("Exception/null API scenario", lines);
    }

    private static void runThreadedScenarios() throws Exception {
        List<QueueCase> cases = queueCases();

        for (int threads : THREAD_COUNTS) {
            List<NamedResult> results = new ArrayList<>();
            for (QueueCase queueCase : cases) {
                results.add(new NamedResult(queueCase.name, runThreaded(queueCase.create(), threads)));
            }

            boolean same = sameThreadedResults(results);

            System.out.println("=== Threaded API scenario: " + threads + " threads ===");
            for (NamedResult result : results) {
                System.out.println(pad(result.name) + " " + result.result);
            }
            System.out.println("same threaded API result across all queues: " + same);
            if (!same) {
                throw new AssertionError("threaded result mismatch for threads=" + threads);
            }
        }
    }

    private static ThreadedResult runThreaded(Queue<Object> queue, int threads) throws Exception {
        Thread[] workers = new Thread[threads];
        CyclicBarrier start = new CyclicBarrier(threads + 1);

        for (int threadId = 0; threadId < threads; threadId++) {
            final int id = threadId;
            workers[threadId] = new Thread(() -> {
                await(start);
                for (int round = 0; round < ROUNDS_PER_THREAD; round++) {
                    int base = id * ROUNDS_PER_THREAD + round;
                    queue.add("s" + base);
                    queue.offer(base);
                    queue.addAll(Arrays.asList("batch" + base, -base));
                }
            }, "api-test-add-" + threadId);
            workers[threadId].start();
        }

        await(start);
        joinAll(workers);

        int expectedBeforeRemove = threads * ROUNDS_PER_THREAD * ENQUEUES_PER_ROUND;
        boolean containsFirstString = queue.contains("s0");
        boolean containsLastInt = queue.contains(threads * ROUNDS_PER_THREAD - 1);
        boolean removeString = queue.remove("batch0");
        boolean removeInt = queue.remove(-(threads * ROUNDS_PER_THREAD - 1));
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

    private static <E> List<NamedQueue<E>> namedQueues() {
        List<NamedQueue<E>> queues = new ArrayList<>();
        queues.add(new NamedQueue<>("lcrq", new java.util.concurrent.LinkedConcurrentRingQueue<>()));
        queues.add(new NamedQueue<>("msqueue", new java.util.concurrent.MSConcurrentLinkedQueue<>()));
        queues.add(new NamedQueue<>("official", new ConcurrentLinkedQueue<>()));
        return queues;
    }

    private static List<QueueCase> queueCases() {
        return Arrays.asList(
                new QueueCase("lcrq", () -> new java.util.concurrent.LinkedConcurrentRingQueue<>()),
                new QueueCase("msqueue", () -> new java.util.concurrent.MSConcurrentLinkedQueue<>()),
                new QueueCase("official", ConcurrentLinkedQueue::new));
    }

    private static <E> List<String> namesOf(List<NamedQueue<E>> queues) {
        List<String> names = new ArrayList<>();
        for (NamedQueue<E> queue : queues) {
            names.add(queue.name);
        }
        return names;
    }

    private static <E> List<Object> apply(List<NamedQueue<E>> queues, QueueOperation<E> operation) {
        List<Object> results = new ArrayList<>();
        for (NamedQueue<E> queue : queues) {
            results.add(operation.apply(queue.queue));
        }
        return results;
    }

    private static List<Integer> integerValues() {
        List<Integer> values = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            values.add((i % 2 == 0) ? i * 7 : -i * 3);
        }
        return values;
    }

    private static List<String> stringValues() {
        List<String> values = new ArrayList<>();
        for (int i = 0; i < 30; i++) {
            values.add("value-" + i + "-" + (char) ('a' + (i % 26)));
        }
        return values;
    }

    private static void compare(List<String> lines, List<String> names, String operation, List<Object> values) {
        boolean same = true;
        Object first = values.get(0);
        for (int i = 1; i < values.size(); i++) {
            same &= Objects.equals(first, values.get(i));
        }

        StringBuilder line = new StringBuilder(operation).append(" -> ");
        for (int i = 0; i < values.size(); i++) {
            if (i > 0) {
                line.append(", ");
            }
            line.append(names.get(i)).append("=").append(summarize(values.get(i)));
        }
        line.append(", same=").append(same);
        lines.add(line.toString());

        if (!same) {
            throw new AssertionError(operation + " mismatch: " + line);
        }
    }

    private static void printSection(String title, List<String> lines) {
        System.out.println("=== " + title + " ===");
        for (String line : lines) {
            System.out.println(line);
        }
    }

    private static Object missingValue() {
        return new Object();
    }

    private static List<Object> listFromIterator(Iterable<?> iterable) {
        List<Object> out = new ArrayList<>();
        for (Object value : iterable) {
            out.add(value);
        }
        return out;
    }

    private static List<Object> listFromSpliterator(Spliterator<?> spliterator) {
        List<Object> out = new ArrayList<>();
        spliterator.forEachRemaining(out::add);
        return out;
    }

    private static List<Object> drain(Queue<?> queue) {
        List<Object> out = new ArrayList<>();
        Object value;
        while ((value = queue.poll()) != null) {
            out.add(value);
        }
        return out;
    }

    private static int countIterator(java.util.Iterator<?> iterator) {
        int count = 0;
        while (iterator.hasNext()) {
            iterator.next();
            count++;
        }
        return count;
    }

    private static int countSpliterator(Spliterator<?> spliterator) {
        final int[] count = {0};
        spliterator.forEachRemaining(value -> count[0]++);
        return count[0];
    }

    private static List<String> sortedStrings(Object[] values) {
        List<String> out = new ArrayList<>();
        for (Object value : values) {
            out.add(String.valueOf(value));
        }
        Collections.sort(out);
        return out;
    }

    private static String exceptionName(Action action) {
        try {
            action.run();
            return "none";
        } catch (Throwable t) {
            return t.getClass().getSimpleName();
        }
    }

    private static boolean sameThreadedResults(List<NamedResult> results) {
        ThreadedResult first = results.get(0).result;
        for (int i = 1; i < results.size(); i++) {
            ThreadedResult current = results.get(i).result;
            if (!first.comparable().equals(current.comparable())
                    || !first.sortedRemaining.equals(current.sortedRemaining)
                    || !first.sortedDrained.equals(current.sortedDrained)) {
                return false;
            }
        }
        return true;
    }

    private static String summarize(Object value) {
        if (!(value instanceof List<?>)) {
            return String.valueOf(value);
        }

        List<?> list = (List<?>) value;
        if (list.size() <= 12) {
            return list.toString();
        }

        return "size=" + list.size() + ", hash=" + list.hashCode() + ", sample=" + sample(list);
    }

    private static String sample(List<?> values) {
        if (values.size() <= 8) {
            return values.toString();
        }

        List<Object> sample = new ArrayList<>();
        sample.addAll(values.subList(0, 4));
        sample.add("...");
        sample.addAll(values.subList(values.size() - 4, values.size()));
        return sample.toString();
    }

    private static String pad(String name) {
        return String.format("%-8s", name);
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

    private interface QueueOperation<E> {
        Object apply(Queue<E> queue);
    }

    private interface Action {
        void run();
    }

    private static final class NamedQueue<E> {
        final String name;
        final Queue<E> queue;

        NamedQueue(String name, Queue<E> queue) {
            this.name = name;
            this.queue = queue;
        }
    }

    private static final class QueueCase {
        final String name;
        final Supplier<Queue<Object>> supplier;

        QueueCase(String name, Supplier<Queue<Object>> supplier) {
            this.name = name;
            this.supplier = supplier;
        }

        Queue<Object> create() {
            return supplier.get();
        }
    }

    private static final class NamedResult {
        final String name;
        final ThreadedResult result;

        NamedResult(String name, ThreadedResult result) {
            this.name = name;
            this.result = result;
        }
    }

    private static final class ThreadedResult {
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

        String comparable() {
            return "expectedBeforeRemove=" + expectedBeforeRemove
                    + ", expectedAfterRemove=" + expectedAfterRemove
                    + ", containsFirstString=" + containsFirstString
                    + ", containsLastInt=" + containsLastInt
                    + ", removeString=" + removeString
                    + ", removeInt=" + removeInt
                    + ", removeMissing=" + removeMissing
                    + ", sizeAfterRemove=" + sizeAfterRemove
                    + ", iteratorCount=" + iteratorCount
                    + ", spliteratorCount=" + spliteratorCount
                    + ", toArrayLength=" + toArrayLength
                    + ", typedToArrayLength=" + typedToArrayLength
                    + ", emptyAfterDrain=" + emptyAfterDrain
                    + ", pollAfterDrain=" + pollAfterDrain;
        }

        @Override
        public String toString() {
            return comparable()
                    + ", sortedRemainingSize=" + sortedRemaining.size()
                    + ", sortedRemainingHash=" + sortedRemaining.hashCode()
                    + ", sortedRemainingSample=" + sample(sortedRemaining)
                    + ", sortedDrainedSize=" + sortedDrained.size()
                    + ", sortedDrainedHash=" + sortedDrained.hashCode()
                    + ", sortedDrainedSample=" + sample(sortedDrained);
        }
    }
}
