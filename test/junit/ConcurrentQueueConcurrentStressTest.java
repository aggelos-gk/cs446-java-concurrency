final class ConcurrentQueueConcurrentStressTest {
    private static final int ROUNDS_PER_THREAD = 1_000;

    static void runAll() throws Exception {
        ConcurrentQueueConcurrentStressTest test = new ConcurrentQueueConcurrentStressTest();
        for (QueueTestSupport.QueueCase queueCase : QueueTestSupport.allQueueCases().toList()) {
            for (int threads : QueueTestSupport.THREAD_COUNTS) {
                test.concurrentApiStressMaintainsContents(queueCase, threads);
            }
        }
    }

    void concurrentApiStressMaintainsContents(QueueTestSupport.QueueCase queueCase, int threads) throws Exception {
        QueueTestSupport.ThreadedResult result = QueueTestSupport.runThreaded(queueCase.create(), threads, ROUNDS_PER_THREAD);

        QueueTestSupport.assertEquals(threads * ROUNDS_PER_THREAD * 4, result.expectedBeforeRemove);
        QueueTestSupport.assertEquals(result.expectedBeforeRemove - 2, result.expectedAfterRemove);
        QueueTestSupport.assertTrue(result.containsFirstString);
        QueueTestSupport.assertTrue(result.containsLastInt);
        QueueTestSupport.assertTrue(result.removeString);
        QueueTestSupport.assertTrue(result.removeInt);
        QueueTestSupport.assertEquals(false, result.removeMissing);
        QueueTestSupport.assertEquals(result.expectedAfterRemove, result.sizeAfterRemove);
        QueueTestSupport.assertEquals(result.expectedAfterRemove, result.iteratorCount);
        QueueTestSupport.assertEquals(result.expectedAfterRemove, result.spliteratorCount);
        QueueTestSupport.assertEquals(result.expectedAfterRemove, result.toArrayLength);
        QueueTestSupport.assertEquals(result.expectedAfterRemove, result.typedToArrayLength);
        QueueTestSupport.assertEquals(result.sortedRemaining, result.sortedDrained);
        QueueTestSupport.assertTrue(result.emptyAfterDrain);
        QueueTestSupport.assertEquals(null, result.pollAfterDrain);
    }
}
