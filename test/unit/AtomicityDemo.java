import java.util.concurrent.atomic.AtomicInteger;

public class AtomicityDemo {

    private static final int ITERATIONS = 1_000_000;

    public static void main(String[] args) throws Exception {

        System.out.println("=== 1. No Synchronization ===");
        noAtomicity();

        System.out.println("\n=== 2. synchronized ===");
        synchronizedCounter();

        System.out.println("\n=== 3. AtomicInteger ===");
        atomicIntegerCounter();

        System.out.println("\n=== 4. CAS ===");
        casCounter();
    }

    // --------------------------------------------------
    // 1. Broken Counter
    // --------------------------------------------------

    static class BrokenCounter {
        int count = 0;

        void increment() {
            count++;
        }
    }

    static void noAtomicity() throws Exception {

        BrokenCounter counter = new BrokenCounter();

        Thread t1 = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.increment();
            }
        });

        Thread t2 = new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                counter.increment();
            }
        });

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Expected: " + (2 * ITERATIONS));
        System.out.println("Actual  : " + counter.count);
    }

    // --------------------------------------------------
    // 2. synchronized
    // --------------------------------------------------

    static class SynchronizedCounter {
        int count = 0;

        synchronized void increment() {
            count++;
        }
    }

    static void synchronizedCounter() throws Exception {

        SynchronizedCounter counter = new SynchronizedCounter();

        Thread t1 = createWorker(counter::increment);
        Thread t2 = createWorker(counter::increment);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Expected: " + (2 * ITERATIONS));
        System.out.println("Actual  : " + counter.count);
    }

    // --------------------------------------------------
    // 3. AtomicInteger
    // --------------------------------------------------

    static class AtomicCounter {
        AtomicInteger count = new AtomicInteger();

        void increment() {
            count.incrementAndGet();
        }
    }

    static void atomicIntegerCounter() throws Exception {

        AtomicCounter counter = new AtomicCounter();

        Thread t1 = createWorker(counter::increment);
        Thread t2 = createWorker(counter::increment);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Expected: " + (2 * ITERATIONS));
        System.out.println("Actual  : " + counter.count.get());
    }

    // --------------------------------------------------
    // 4. CAS
    // --------------------------------------------------

    static class CASCounter {

        AtomicInteger count = new AtomicInteger();

        void increment() {

            while (true) {

                int current = count.get();

                int next = current + 1;

                if (count.compareAndSet(current, next)) {
                    return;
                }
            }
        }
    }

    static void casCounter() throws Exception {

        CASCounter counter = new CASCounter();

        Thread t1 = createWorker(counter::increment);
        Thread t2 = createWorker(counter::increment);

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("Expected: " + (2 * ITERATIONS));
        System.out.println("Actual  : " + counter.count.get());
    }

    // --------------------------------------------------

    static Thread createWorker(Runnable task) {

        return new Thread(() -> {
            for (int i = 0; i < ITERATIONS; i++) {
                task.run();
            }
        });
    }
}