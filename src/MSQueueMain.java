public final class MSQueueMain {
    private static final int OPERATIONS = 1_000_000;

    private MSQueueMain() {
    }

    public static void main(String[] args) {
        MSLinkedConcurrentQueue<Integer> demoQueue = new MSLinkedConcurrentQueue<>();
        System.out.print(QueueDemoSupport.functionalOutput(demoQueue));

        long nanos = QueueDemoSupport.timeOfferPollNanos(new MSLinkedConcurrentQueue<>(), OPERATIONS);
        System.out.printf("msqueue time for %,d sequential offer+poll pairs: %.3f ms%n",
                OPERATIONS, QueueDemoSupport.millis(nanos));
    }
}
