public final class CustomQueueMain {
    private static final int OPERATIONS = 1_000_000;

    private CustomQueueMain() {
    }

    public static void main(String[] args) {
        LinkedConcurrentRingQueue<Integer> demoQueue = new LinkedConcurrentRingQueue<>();
        System.out.print(QueueDemoSupport.functionalOutput(demoQueue));

        long nanos = QueueDemoSupport.timeOfferPollNanos(new LinkedConcurrentRingQueue<>(), OPERATIONS);
        System.out.printf("custom time for %,d sequential offer+poll pairs: %.3f ms%n",
                OPERATIONS, QueueDemoSupport.millis(nanos));
    }
}
