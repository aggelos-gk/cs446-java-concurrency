public final class ConcurrentQueueLibraryTestMain {
    private ConcurrentQueueLibraryTestMain() {
    }

    public static void main(String[] args) throws Exception {
        ConcurrentQueueSequentialApiTest.runAll();
        System.out.println("ConcurrentQueueSequentialApiTest passed");

        ConcurrentQueueConcurrentStressTest.runAll();
        System.out.println("ConcurrentQueueConcurrentStressTest passed");

        ConcurrentQueueRandomizedDifferentialTest.runAll();
        System.out.println("ConcurrentQueueRandomizedDifferentialTest passed");
    }
}
