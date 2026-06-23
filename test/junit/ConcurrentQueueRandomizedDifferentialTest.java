import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.Random;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

final class ConcurrentQueueRandomizedDifferentialTest {
    private static final int SEEDS = 25;
    private static final int OPERATIONS_PER_SEED = 1_000;

    static Stream<QueueTestSupport.QueueCase> customQueues() {
        return QueueTestSupport.customQueueCases();
    }

    static void runAll() {
        ConcurrentQueueRandomizedDifferentialTest test = new ConcurrentQueueRandomizedDifferentialTest();
        for (QueueTestSupport.QueueCase queueCase : customQueues().toList()) {
            test.randomizedApiBehaviorMatchesConcurrentLinkedQueue(queueCase);
            test.randomizedNullExceptionsMatchReference(queueCase);
        }
    }

    void randomizedApiBehaviorMatchesConcurrentLinkedQueue(QueueTestSupport.QueueCase queueCase) {
        for (int seed = 0; seed < SEEDS; seed++) {
            Queue<Object> actual = queueCase.create();
            Queue<Object> expected = new ConcurrentLinkedQueue<>();
            Random random = new Random(0xC0FFEE + seed);

            for (int op = 0; op < OPERATIONS_PER_SEED; op++) {
                Object value = randomValue(random);
                switch (random.nextInt(14)) {
                    case 0:
                        QueueTestSupport.assertEquals(expected.add(value), actual.add(value), context(seed, op, "add"));
                        break;
                    case 1:
                        QueueTestSupport.assertEquals(expected.offer(value), actual.offer(value), context(seed, op, "offer"));
                        break;
                    case 2:
                        List<Object> batch = Arrays.asList(value, randomValue(random), randomValue(random));
                        QueueTestSupport.assertEquals(expected.addAll(batch), actual.addAll(batch), context(seed, op, "addAll"));
                        break;
                    case 3:
                        QueueTestSupport.assertEquals(expected.peek(), actual.peek(), context(seed, op, "peek"));
                        break;
                    case 4:
                        QueueTestSupport.assertEquals(expected.poll(), actual.poll(), context(seed, op, "poll"));
                        break;
                    case 5:
                        QueueTestSupport.assertEquals(expected.contains(value), actual.contains(value), context(seed, op, "contains"));
                        break;
                    case 6:
                        QueueTestSupport.assertEquals(expected.remove(value), actual.remove(value), context(seed, op, "remove"));
                        break;
                    case 7:
                        QueueTestSupport.assertEquals(expected.size(), actual.size(), context(seed, op, "size"));
                        break;
                    case 8:
                        QueueTestSupport.assertEquals(expected.isEmpty(), actual.isEmpty(), context(seed, op, "isEmpty"));
                        break;
                    case 9:
                        QueueTestSupport.assertEquals(QueueTestSupport.listFromIterator(expected), QueueTestSupport.listFromIterator(actual), context(seed, op, "iterator"));
                        break;
                    case 10:
                        QueueTestSupport.assertEquals(QueueTestSupport.listFromSpliterator(expected.spliterator()), QueueTestSupport.listFromSpliterator(actual.spliterator()), context(seed, op, "spliterator"));
                        break;
                    case 11:
                        QueueTestSupport.assertEquals(Arrays.asList(expected.toArray()), Arrays.asList(actual.toArray()), context(seed, op, "toArray"));
                        break;
                    case 12:
                        QueueTestSupport.assertEquals(Arrays.asList(expected.toArray(new Object[0])), Arrays.asList(actual.toArray(new Object[0])), context(seed, op, "toArray small"));
                        break;
                    case 13:
                        int extra = random.nextInt(8);
                        QueueTestSupport.assertEquals(Arrays.asList(expected.toArray(new Object[expected.size() + extra])),
                                Arrays.asList(actual.toArray(new Object[actual.size() + extra])),
                                context(seed, op, "toArray large"));
                        break;
                    default:
                        throw new AssertionError("unreachable");
                }
            }

            QueueTestSupport.assertEquals(QueueTestSupport.drain(expected), QueueTestSupport.drain(actual), "final drain mismatch seed=" + seed);
        }
    }

    void randomizedNullExceptionsMatchReference(QueueTestSupport.QueueCase queueCase) {
        Queue<Object> queue = queueCase.create();
        QueueTestSupport.assertThrows(NullPointerException.class, () -> queue.add(null));
        QueueTestSupport.assertThrows(NullPointerException.class, () -> queue.offer(null));
        QueueTestSupport.assertEquals(false, queue.contains(null));
        QueueTestSupport.assertEquals(false, queue.remove(null));
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
