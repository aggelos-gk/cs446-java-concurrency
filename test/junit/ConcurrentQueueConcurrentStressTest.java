import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.stream.IntStream;
import java.util.stream.Stream;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

final class ConcurrentQueueConcurrentStressTest {
    private static final int ROUNDS_PER_THREAD = 1_000;

    static Stream<Arguments> queueAndThreadCounts() {
        return QueueTestSupport.allQueueCases().flatMap(queueCase ->
                IntStream.of(QueueTestSupport.THREAD_COUNTS).mapToObj(threads -> Arguments.of(queueCase, threads)));
    }

    @ParameterizedTest(name = "{0} concurrent API stress with {1} threads")
    @MethodSource("queueAndThreadCounts")
    void concurrentApiStressMaintainsContents(QueueTestSupport.QueueCase queueCase, int threads) throws Exception {
        QueueTestSupport.ThreadedResult result = QueueTestSupport.runThreaded(queueCase.create(), threads, ROUNDS_PER_THREAD);

        assertEquals(threads * ROUNDS_PER_THREAD * 4, result.expectedBeforeRemove);
        assertEquals(result.expectedBeforeRemove - 2, result.expectedAfterRemove);
        assertTrue(result.containsFirstString);
        assertTrue(result.containsLastInt);
        assertTrue(result.removeString);
        assertTrue(result.removeInt);
        assertEquals(false, result.removeMissing);
        assertEquals(result.expectedAfterRemove, result.sizeAfterRemove);
        assertEquals(result.expectedAfterRemove, result.iteratorCount);
        assertEquals(result.expectedAfterRemove, result.spliteratorCount);
        assertEquals(result.expectedAfterRemove, result.toArrayLength);
        assertEquals(result.expectedAfterRemove, result.typedToArrayLength);
        assertEquals(result.sortedRemaining, result.sortedDrained);
        assertTrue(result.emptyAfterDrain);
        assertEquals(null, result.pollAfterDrain);
    }
}
