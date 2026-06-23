# Extend Java's Concurrent Library with More Concurrent Structures

CS446 course project.

This project is about extending Java's concurrent library with two extra queue implementations and then checking both correctness and performance.

The OpenJDK repo used for the library integration lives here:

- `https://github.com/aggelos-gk/jdk21u`

The two main queues are:

- `MSLinkedConcurrentQueue`: a Michael-Scott-style lock-free linked queue prototype in `src/`
- `LinkedConcurrentRingQueue`: an LCRQ-inspired segmented queue

The prototype files also exist in this repo under `src/`, but to use them as real `java.util.concurrent` library classes they need to be placed into the OpenJDK tree and the JDK needs to be built.

For the JDK integration, the source files from `src/` that matter need to end up under:

- `/home/aggelos/Git_repos/jdk21u/src/java.base/share/classes/java/util/concurrent/`

That is the `java.util.concurrent` source folder inside the local OpenJDK tree. In practice, the queue code from `src/MSLinkedConcurrentQueue.java` and `src/LinkedConcurrentRingQueue.java` is adapted there so the custom JDK build can use them like normal library classes.

In general there were many fail attempts before getting here. That was expected. Concurrent queues look simple on paper, but in practice small mistakes in CAS ordering, helping logic, or slot state handling break things very fast.

## Repo layout

- `src/`: local prototype implementations and older demo code
- `test/junit/`: custom correctness tests that compare behavior with `ConcurrentLinkedQueue`
- `test/jcstress/`: small concurrency race tests
- `test/benchmarks/`: JMH benchmarks
- `test/benchmarks/custom/`: simple custom timing / comparison programs
- `paper/`: report and figures
- `../jdk21u/`: local OpenJDK tree where the new queues were added into `java.util.concurrent`

## Setup commands

Build the custom JDK:

```bash
cd /home/aggelos/Git_repos/jdk21u
make CONF=linux-x86_64-server-release images
```

Copy / adapt the queue source files into the JDK concurrent package:

```bash
cp /home/aggelos/Git_repos/cs446-java-concurrency/src/MSLinkedConcurrentQueue.java \
   /home/aggelos/Git_repos/jdk21u/src/java.base/share/classes/java/util/concurrent/

cp /home/aggelos/Git_repos/cs446-java-concurrency/src/LinkedConcurrentRingQueue.java \
   /home/aggelos/Git_repos/jdk21u/src/java.base/share/classes/java/util/concurrent/
```

If needed, adjust package / class naming there so the JDK versions match the final library names used in `java.util.concurrent`.

Use that JDK for everything else:

```bash
export JAVA_HOME=/home/aggelos/Git_repos/jdk21u/build/linux-x86_64-server-release/images/jdk
export PATH="$JAVA_HOME/bin:/home/aggelos/.local/bin:$PATH"
```

Run the custom correctness tests:

```bash
cd /home/aggelos/Git_repos/cs446-java-concurrency
gradle --no-daemon libraryTest
```

Run the JCStress tests:

```bash
cd /home/aggelos/Git_repos/cs446-java-concurrency
gradle --no-daemon jcstress
```

Run the JMH benchmarks:

```bash
cd /home/aggelos/Git_repos/cs446-java-concurrency
gradle --no-daemon jmh
```

Run a quick JMH benchmark only for the round-trip case:

```bash
cd /home/aggelos/Git_repos/cs446-java-concurrency
gradle --no-daemon jmh -PjmhArgs="benchmarks.QueueRoundTripBenchmark.offerPollCycle -p implementation=jdk,ms,lcrq -f 1 -wi 1 -i 1 -w 200ms -r 200ms -t 8"
```

Run the simple custom benchmark programs:

```bash
cd /home/aggelos/Git_repos/cs446-java-concurrency
javac src/*.java test/benchmarks/custom/*.java
java -cp src:test/benchmarks/custom QueueComparisonMain
```

Build the paper:

```bash
cd /home/aggelos/Git_repos/cs446-java-concurrency/paper
make
```

## What is in `test/`

There are basically three kinds of tests here:

- behavior / API tests: check that the custom queues behave the same way as `ConcurrentLinkedQueue`
- stress tests: use JCStress for small concurrent interleavings
- benchmarks: measure sequential and concurrent performance

The custom timing code in `test/benchmarks/custom/` is intentionally simple and readable. It was useful early on because it was easier to debug than jumping directly into JMH.

The JMH benchmarks in `test/benchmarks/` are there for cleaner performance measurements. Right now they include:

- `QueueRoundTripBenchmark`: one `offer()` followed by one `poll()`
- `QueueProducerConsumerBenchmark`: producer / consumer groups with multiple thread layouts

## Fail attempts and things learned

There were a lot of fail attempts overall. Two of the most useful ones were these:

### 1. MS queue `offer()` bug

In one earlier version of the MS-style queue, the first two checks inside `offer()` were not there in the right way:

- `if (t == tail)`
- `if (next == null)`

That means the code could try to link a new node based on a stale tail snapshot, or behave as if the current tail was still the real last node when another thread had already changed the queue. The result was wrong functional output and mismatches against the reference queue under contention. After putting those checks back in the normal Michael-Scott style, the queue stopped making those stale decisions and the behavior became stable.

### 2. LCRQ-inspired queue empty-slot mistake

One of the easy mistakes in the segmented queue was treating an `EMPTY` slot as if the queue was definitely empty right away. That is not always true. A producer may already have reserved positions with `getAndAdd`, but not yet published the actual element into the array slot. If `poll()` gives up too early there, the consumer can report empty too soon, and then the threaded tests start showing bad counts, bad checksums, or remaining elements in the queue after the run.

The fix was to re-check the segment tail and keep the logic around `EMPTY`, `SKIPPED`, and reservation order more careful. That was one of the places where the segmented queue was much more subtle than it first looked.

### 3. Helping logic matters more than expected

Another general lesson was that helping logic is not optional detail. In both queue styles, if head/tail movement or segment movement is not helped correctly, the code may still look almost right, but throughput falls fast and threads spend too much time retrying CAS operations. A lot of the final behavior came down to these small details.

## Current result in one sentence

The custom queues now build inside the local OpenJDK 21 tree, pass the custom behavior tests, pass the JCStress runs that were added here, and can be benchmarked with both simple custom timing code and JMH.
