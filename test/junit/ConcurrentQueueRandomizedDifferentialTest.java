import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class ConcurrentQueueRandomizedDifferentialTest {
    private static final int SEEDS = 25;
    private static final int OPERATIONS_PER_SEED = 1_000;

    static Stream<QueueTestSupport.QueueCase> customQueues() {
        return QueueTestSupport.customQueueCases();
    }

    @ParameterizedTest(name = "{0} randomized API behavior matches ConcurrentLinkedQueue")
    @MethodSource("customQueues")
    void randomizedApiBehaviorMatchesConcurrentLinkedQueue(QueueTestSupport.QueueCase queueCase) {
        for (int seed = 0; seed < SEEDS; seed++) {
            Queue<Object> actual = queueCase.create();
            Queue<Object> expected = new ConcurrentLinkedQueue<>();
            Random random = new Random(0xC0FFEE + seed);

            for (int op = 0; op < OPERATIONS_PER_SEED; op++) {
                Object value = randomValue(random);
                switch (random.nextInt(14)) {
                    case 0:
                        assertEquals(expected.add(value), actual.add(value), context(seed, op, "add"));
                        break;
                    case 1:
                        assertEquals(expected.offer(value), actual.offer(value), context(seed, op, "offer"));
                        break;
                    case 2:
                        List<Object> batch = Arrays.asList(value, randomValue(random), randomValue(random));
                        assertEquals(expected.addAll(batch), actual.addAll(batch), context(seed, op, "addAll"));
                        break;
                    case 3:
                        assertEquals(expected.peek(), actual.peek(), context(seed, op, "peek"));
                        break;
                    case 4:
                        assertEquals(expected.poll(), actual.poll(), context(seed, op, "poll"));
                        break;
                    case 5:
                        assertEquals(expected.contains(value), actual.contains(value), context(seed, op, "contains"));
                        break;
                    case 6:
                        assertEquals(expected.remove(value), actual.remove(value), context(seed, op, "remove"));
                        break;
                    case 7:
                        assertEquals(expected.size(), actual.size(), context(seed, op, "size"));
                        break;
                    case 8:
                        assertEquals(expected.isEmpty(), actual.isEmpty(), context(seed, op, "isEmpty"));
                        break;
                    case 9:
                        assertEquals(QueueTestSupport.listFromIterator(expected), QueueTestSupport.listFromIterator(actual), context(seed, op, "iterator"));
                        break;
                    case 10:
                        assertEquals(QueueTestSupport.listFromSpliterator(expected.spliterator()), QueueTestSupport.listFromSpliterator(actual.spliterator()), context(seed, op, "spliterator"));
                        break;
                    case 11:
                        assertEquals(Arrays.asList(expected.toArray()), Arrays.asList(actual.toArray()), context(seed, op, "toArray"));
                        break;
                    case 12:
                        assertEquals(Arrays.asList(expected.toArray(new Object[0])), Arrays.asList(actual.toArray(new Object[0])), context(seed, op, "toArray small"));
                        break;
                    case 13:
                        int extra = random.nextInt(8);
                        assertEquals(Arrays.asList(expected.toArray(new Object[expected.size() + extra])),
                                Arrays.asList(actual.toArray(new Object[actual.size() + extra])),
                                context(seed, op, "toArray large"));
                        break;
                    default:
                        throw new AssertionError("unreachable");
                }
            }

            assertEquals(QueueTestSupport.drain(expected), QueueTestSupport.drain(actual), "final drain mismatch seed=" + seed);
        }
    }

    @ParameterizedTest(name = "{0} randomized null exceptions match ConcurrentLinkedQueue")
    @MethodSource("customQueues")
    void randomizedNullExceptionsMatchReference(QueueTestSupport.QueueCase queueCase) {
        Queue<Object> queue = queueCase.create();
        assertThrows(NullPointerException.class, () -> queue.add(null));
        assertThrows(NullPointerException.class, () -> queue.offer(null));
        assertEquals(false, queue.contains(null));
        assertEquals(false, queue.remove(null));
    }

    private static Object randomValue(Random random) {
        int n = random.nextInt(128);
        switch (random.nextInt(5)) {
            case 0:
                return n;
            case 1:
                return -n;
            case 2:
                return "s" + n;
            case 3:
                return "batch" + (n % 17);
            case 4:
                return (long) n * 31L;
            default:
                throw new AssertionError("unreachable");
        }
    }

    private static String context(int seed, int op, String operation) {
        return "seed=" + seed + ", op=" + op + ", operation=" + operation;
    }
}
