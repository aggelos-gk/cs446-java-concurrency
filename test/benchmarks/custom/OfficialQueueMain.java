import java.util.concurrent.ConcurrentLinkedQueue;

public final class OfficialQueueMain {
    private static final int OPERATIONS = 1_000_000;

    private OfficialQueueMain() {
    }

    public static void main(String[] args) {
        ConcurrentLinkedQueue<Integer> demoQueue = new ConcurrentLinkedQueue<>();
        System.out.print(QueueDemoSupport.functionalOutput(demoQueue));

        long nanos = QueueDemoSupport.timeOfferPollNanos(new ConcurrentLinkedQueue<>(), OPERATIONS);
        System.out.printf("official time for %,d sequential offer+poll pairs: %.3f ms%n",
                OPERATIONS, QueueDemoSupport.millis(nanos));
    }
}
