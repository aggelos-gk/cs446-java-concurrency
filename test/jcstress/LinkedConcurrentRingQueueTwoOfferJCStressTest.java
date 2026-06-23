package jcstress;

import java.util.Queue;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

@JCStressTest
@Outcome(id = "1, 2", expect = ACCEPTABLE, desc = "offer(1) linearized before offer(2)")
@Outcome(id = "2, 1", expect = ACCEPTABLE, desc = "offer(2) linearized before offer(1)")
@State
public class LinkedConcurrentRingQueueTwoOfferJCStressTest {
    private final Queue<Integer> queue = newQueue("LinkedConcurrentRingQueue");

    @Actor
    public void actor1() {
        queue.offer(1);
    }

    @Actor
    public void actor2() {
        queue.offer(2);
    }

    @Arbiter
    public void arbiter(II_Result r) {
        r.r1 = value(queue.poll());
        r.r2 = value(queue.poll());
    }

    private static int value(Integer value) {
        return value == null ? -1 : value;
    }

    @SuppressWarnings("unchecked")
    private static Queue<Integer> newQueue(String className) {
        try {
            return (Queue<Integer>) Class.forName(className).getConstructor().newInstance();
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
}
