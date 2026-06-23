package jcstress;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

@JCStressTest
@Outcome(id = "1, -1", expect = ACCEPTABLE, desc = "poll observes the offered element")
@Outcome(id = "-1, 1", expect = ACCEPTABLE, desc = "poll linearizes before offer; arbiter later observes the element")
@State
public class OfficialConcurrentLinkedQueueOfferPollJCStressTest {
    private final ConcurrentLinkedQueue<Integer> queue = new ConcurrentLinkedQueue<>();

    @Actor
    public void actor1() {
        queue.offer(1);
    }

    @Actor
    public void actor2(II_Result r) {
        r.r1 = value(queue.poll());
    }

    @Arbiter
    public void arbiter(II_Result r) {
        r.r2 = value(queue.poll());
    }

    private static int value(Integer value) {
        return value == null ? -1 : value;
    }
}
