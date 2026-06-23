import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

final class ConcurrentQueueSequentialApiTest {
    static Stream<QueueTestSupport.QueueCase> queues() {
        return QueueTestSupport.allQueueCases();
    }

    @ParameterizedTest(name = "{0} integer API matches FIFO semantics")
    @MethodSource("queues")
    void integerApiMatchesFifoSemantics(QueueTestSupport.QueueCase queueCase) {
        assertSequentialScenario(queueCase.create(), new ArrayList<>(QueueTestSupport.integerValues()));
    }

    @ParameterizedTest(name = "{0} string API matches FIFO semantics")
    @MethodSource("queues")
    void stringApiMatchesFifoSemantics(QueueTestSupport.QueueCase queueCase) {
        assertSequentialScenario(queueCase.create(), new ArrayList<>(QueueTestSupport.stringValues()));
    }

    @ParameterizedTest(name = "{0} mixed Object API matches ConcurrentLinkedQueue")
    @MethodSource("queues")
    void mixedObjectApiMatchesReference(QueueTestSupport.QueueCase queueCase) {
        Queue<Object> actual = queueCase.create();
        Queue<Object> expected = new ConcurrentLinkedQueue<>();
        List<Object> mixed = QueueTestSupport.mixedValues();

        assertEquals(expected.add("zero"), actual.add("zero"));
        assertEquals(expected.addAll(mixed), actual.addAll(mixed));
        assertEquals(expected.offer("tail"), actual.offer("tail"));
        assertEquals(expected.contains("three"), actual.contains("three"));
        assertEquals(expected.contains(4), actual.contains(4));
        assertEquals(expected.contains(20.75), actual.contains(20.75));
        assertEquals(expected.remove(6L), actual.remove(6L));
        assertEquals(expected.remove(-18L), actual.remove(-18L));
        assertEquals(QueueTestSupport.listFromIterator(expected), QueueTestSupport.listFromIterator(actual));
        assertEquals(Arrays.asList(expected.toArray()), Arrays.asList(actual.toArray()));
        assertEquals(QueueTestSupport.drain(expected), QueueTestSupport.drain(actual));
        assertTrue(actual.isEmpty());
    }

    @ParameterizedTest(name = "{0} null behavior matches ConcurrentLinkedQueue")
    @MethodSource("queues")
    void nullBehaviorMatchesReference(QueueTestSupport.QueueCase queueCase) {
        Queue<Object> queue = queueCase.create();

        assertThrows(NullPointerException.class, () -> queue.offer(null));
        assertThrows(NullPointerException.class, () -> queue.add(null));
        assertThrows(IllegalArgumentException.class, () -> queue.addAll(queue));
        assertFalse(queue.contains(null));
        assertFalse(queue.remove(null));
    }

    private static <E> void assertSequentialScenario(Queue<Object> queue, List<E> values) {
        assertTrue(queue.isEmpty());
        assertTrue(queue.add(values.get(0)));
        assertTrue(queue.offer(values.get(1)));
        assertTrue(queue.addAll(values.subList(2, values.size())));
        assertEquals(values.get(0), queue.peek());
        assertTrue(queue.contains(values.get(0)));
        assertTrue(queue.contains(values.get(values.size() / 2)));
        assertFalse(queue.contains(new Object()));
        assertTrue(queue.remove(values.get(2)));
        assertTrue(queue.remove(values.get(values.size() / 2)));
        assertFalse(queue.remove(new Object()));

        List<E> expected = new ArrayList<>(values);
        expected.remove(values.get(2));
        expected.remove(values.get(values.size() / 2));

        assertEquals(expected.size(), queue.size());
        assertEquals(expected, QueueTestSupport.listFromIterator(queue));
        assertEquals(expected, QueueTestSupport.listFromSpliterator(queue.spliterator()));
        assertEquals(expected, Arrays.asList(queue.toArray()));
        assertEquals(expected, Arrays.asList(queue.toArray(new Object[0])));

        Object[] large = queue.toArray(new Object[expected.size() + 5]);
        Object[] expectedLarge = new Object[expected.size() + 5];
        for (int i = 0; i < expected.size(); i++) {
            expectedLarge[i] = expected.get(i);
        }
        assertArrayEquals(expectedLarge, large);

        assertEquals(expected.remove(0), queue.poll());
        assertEquals(expected.remove(0), queue.poll());
        assertEquals(expected.remove(0), queue.poll());
        assertEquals(expected.get(0), queue.peek());
        assertEquals(expected, QueueTestSupport.drain(queue));
        assertEquals(null, queue.poll());
        assertTrue(queue.isEmpty());
    }
}
