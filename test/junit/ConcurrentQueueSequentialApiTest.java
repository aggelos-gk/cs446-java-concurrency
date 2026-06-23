import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

final class ConcurrentQueueSequentialApiTest {
    static Stream<QueueTestSupport.QueueCase> queues() {
        return QueueTestSupport.allQueueCases();
    }

    static void runAll() {
        ConcurrentQueueSequentialApiTest test = new ConcurrentQueueSequentialApiTest();
        for (QueueTestSupport.QueueCase queueCase : queues().toList()) {
            test.integerApiMatchesFifoSemantics(queueCase);
            test.stringApiMatchesFifoSemantics(queueCase);
            test.mixedObjectApiMatchesReference(queueCase);
            test.nullBehaviorMatchesReference(queueCase);
        }
    }

    void integerApiMatchesFifoSemantics(QueueTestSupport.QueueCase queueCase) {
        assertSequentialScenario(queueCase.create(), new ArrayList<>(QueueTestSupport.integerValues()));
    }

    void stringApiMatchesFifoSemantics(QueueTestSupport.QueueCase queueCase) {
        assertSequentialScenario(queueCase.create(), new ArrayList<>(QueueTestSupport.stringValues()));
    }

    void mixedObjectApiMatchesReference(QueueTestSupport.QueueCase queueCase) {
        Queue<Object> actual = queueCase.create();
        Queue<Object> expected = new ConcurrentLinkedQueue<>();
        List<Object> mixed = QueueTestSupport.mixedValues();

        QueueTestSupport.assertEquals(expected.add("zero"), actual.add("zero"));
        QueueTestSupport.assertEquals(expected.addAll(mixed), actual.addAll(mixed));
        QueueTestSupport.assertEquals(expected.offer("tail"), actual.offer("tail"));
        QueueTestSupport.assertEquals(expected.contains("three"), actual.contains("three"));
        QueueTestSupport.assertEquals(expected.contains(4), actual.contains(4));
        QueueTestSupport.assertEquals(expected.contains(20.75), actual.contains(20.75));
        QueueTestSupport.assertEquals(expected.remove(6L), actual.remove(6L));
        QueueTestSupport.assertEquals(expected.remove(-18L), actual.remove(-18L));
        QueueTestSupport.assertEquals(QueueTestSupport.listFromIterator(expected), QueueTestSupport.listFromIterator(actual));
        QueueTestSupport.assertEquals(Arrays.asList(expected.toArray()), Arrays.asList(actual.toArray()));
        QueueTestSupport.assertEquals(QueueTestSupport.drain(expected), QueueTestSupport.drain(actual));
        QueueTestSupport.assertTrue(actual.isEmpty());
    }

    void nullBehaviorMatchesReference(QueueTestSupport.QueueCase queueCase) {
        Queue<Object> queue = queueCase.create();

        QueueTestSupport.assertThrows(NullPointerException.class, () -> queue.offer(null));
        QueueTestSupport.assertThrows(NullPointerException.class, () -> queue.add(null));
        QueueTestSupport.assertThrows(IllegalArgumentException.class, () -> queue.addAll(queue));
        QueueTestSupport.assertFalse(queue.contains(null));
        QueueTestSupport.assertFalse(queue.remove(null));
    }

    private static <E> void assertSequentialScenario(Queue<Object> queue, List<E> values) {
        QueueTestSupport.assertTrue(queue.isEmpty());
        QueueTestSupport.assertTrue(queue.add(values.get(0)));
        QueueTestSupport.assertTrue(queue.offer(values.get(1)));
        QueueTestSupport.assertTrue(queue.addAll(values.subList(2, values.size())));
        QueueTestSupport.assertEquals(values.get(0), queue.peek());
        QueueTestSupport.assertTrue(queue.contains(values.get(0)));
        QueueTestSupport.assertTrue(queue.contains(values.get(values.size() / 2)));
        QueueTestSupport.assertFalse(queue.contains(new Object()));
        QueueTestSupport.assertTrue(queue.remove(values.get(2)));
        QueueTestSupport.assertTrue(queue.remove(values.get(values.size() / 2)));
        QueueTestSupport.assertFalse(queue.remove(new Object()));

        List<E> expected = new ArrayList<>(values);
        expected.remove(values.get(2));
        expected.remove(values.get(values.size() / 2));

        QueueTestSupport.assertEquals(expected.size(), queue.size());
        QueueTestSupport.assertEquals(expected, QueueTestSupport.listFromIterator(queue));
        QueueTestSupport.assertEquals(expected, QueueTestSupport.listFromSpliterator(queue.spliterator()));
        QueueTestSupport.assertEquals(expected, Arrays.asList(queue.toArray()));
        QueueTestSupport.assertEquals(expected, Arrays.asList(queue.toArray(new Object[0])));

        Object[] large = queue.toArray(new Object[expected.size() + 5]);
        Object[] expectedLarge = new Object[expected.size() + 5];
        for (int i = 0; i < expected.size(); i++) {
            expectedLarge[i] = expected.get(i);
        }
        QueueTestSupport.assertArrayEquals(expectedLarge, large);

        QueueTestSupport.assertEquals(expected.remove(0), queue.poll());
        QueueTestSupport.assertEquals(expected.remove(0), queue.poll());
        QueueTestSupport.assertEquals(expected.remove(0), queue.poll());
        QueueTestSupport.assertEquals(expected.get(0), queue.peek());
        QueueTestSupport.assertEquals(expected, QueueTestSupport.drain(queue));
        QueueTestSupport.assertEquals(null, queue.poll());
        QueueTestSupport.assertTrue(queue.isEmpty());
    }
}
