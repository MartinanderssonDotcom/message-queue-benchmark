package com.martinandersson.qsb.impl;

import com.martinandersson.qsb.api.Message;
import com.martinandersson.qsb.api.QueueService;
import java.lang.reflect.Method;
import java.time.Duration;
import static java.util.Arrays.asList;
import static java.util.Arrays.copyOf;
import static java.util.Arrays.stream;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Function;
import static java.util.stream.Collectors.counting;
import static java.util.stream.Collectors.groupingBy;
import java.util.stream.IntStream;
import static java.util.stream.Stream.concat;
import static org.testng.Assert.assertEquals;
import static org.testng.Assert.assertNull;
import static org.testng.Assert.fail;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

/**
 * Loads of unit tests for any {@code QueueService} implementation.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
// I prefer to annotate my methods with @Test - more explicit that way.
// But, a bug force me to put this annotation on the class.
// See: https://github.com/cbeust/testng/issues/1222#issuecomment-289232703
@Test
public abstract class AbstractQSTest
{
    protected abstract Function<Duration, QueueService> getFactory();
    
    private QueueService testee;
    
    
    
    @BeforeMethod
    public void before_method(Method method) {
        given_timeout(10);
        
    }
    
    
    
    public void test_one_push_poll_remove() {
        testee.push("hello", "world");
        
        Message msg = testee.poll("hello");
        
        assertEquals(msg.queue(), "hello");
        assertEquals(msg.get(), "world");
        
        // No more stuff in the queue.
        assertNull(testee.poll("hello"));
        
        // We only try to survive here. Could add some more checks below (no
        // weird side-effects like adding a message et cetera).
        testee.complete(msg);
    }
    
    public void test_two_queues() {
        testee.push("q1", "m1");
        testee.push("q2", "m2");
        
        assert_queue_content("q1", "m1");
        assert_queue_content("q2", "m2");
    }
    
    /**
     * Duplicate messages are allowed. Every invocation of a push() translates
     * to a new message.
     */
    public void test_duplicates() {
        final String second = new String("m");
        
        testee.push("q", "m", second, "m");
        
        assert_queue_content("q", "m", second, "m");
    }
    
    public void test_remove_singleton() {
        testee.push("remove", "me!");
        testee.complete(testee.poll("remove"));
        
        // Assert queue is empty:
        assert_queue_content("remove");
    }
    
    public void test_remove_first() {
        testee.push("q", "m1", "m2");
        testee.complete(testee.poll("q"));
        
        assert_queue_content("q", "m2");
    }
    
    public void test_remove_man_in_the_middle() {
        testee.push("q", "m1", "m2", "m3");
        testee.poll("q");
        testee.complete(testee.poll("q"));
        
        assert_queue_content("q", "m3");
    }
    
    public void test_remove_last() throws InterruptedException {
        testee.push("q", "m1", "m2");
        
        testee.poll("q");
        testee.complete(testee.poll("q"));
        
        assert_queue_content("q");
    }
    
    public void test_timeout() {
        // timeout immediately lol - what a great feature!
        given_timeout(0);
        
        testee.push("q", "m");
        
        assertEquals(testee.poll("q"), testee.poll("q"));
    }
    
    
    
    protected int getQueues() {
        return 100;
    }
    
    protected int getMessagesPerThread() {
        return 50_000;
    }
    
    /**
     * Will under very high contention poll tons of messages through testee and
     * assert that each message was delivered at least once.<p>
     * 
     * Warning! This is a method that may cause you headache. Read at your own
     * risk! I needed to have a go at the delivery semantics.
     * 
     * @throws InterruptedException  if interrupted
     */
    public void test_at_least_once() throws InterruptedException {
        given_timeout(Integer.MAX_VALUE);
        
        final int THREADS = (int) Math.pow(Runtime.getRuntime().availableProcessors(), 2),
                  QUEUES  = getQueues(),
                  MESSAGES_PER_THREAD = getMessagesPerThread();
        
        if (THREADS == 1) {
            throw new UnsupportedOperationException("Please upgrade your machine.");
        }
        
        String[] queues   = mkStrings(QUEUES, "Q"),
                 messages = mkStrings(MESSAGES_PER_THREAD, "M");
        
        CountDownLatch wait = new CountDownLatch(THREADS); // <-- ONLY to be safe.
        
        Thread[] producers = mkThreads(THREADS, () -> stream(messages).forEach(msg -> {
            wait.countDown();
            // ..message has to be unique because that's how frequencies are counted.
            // (can't use Message as key, equality across queues not supported)
            testee.push(rand(queues), "T" + Thread.currentThread().getId() + msg);
        }));
        
        final Map<String, LongAdder> freq = new ConcurrentHashMap<>();
        
        Thread[] consumers = mkThreads(THREADS, () -> {
            try {
                wait.await(2, TimeUnit.SECONDS);
            }
            catch (InterruptedException e) {
                return;
            }
            
            List<String> myPollOrder = asList(copyOf(queues, queues.length));
            Collections.shuffle(myPollOrder);
            
            Message m;
            
            do {
                m = null;
                
                for (String q : myPollOrder) {
                    Message m0 = testee.poll(q);
                    
                    if (m0 == null) {
                        continue;
                    }
                    
                    m = m0;
                    
                    freq.computeIfAbsent(m.get(), x -> new LongAdder()).increment();
                    testee.complete(m);
                }
            }
            // Kill consumer when all producers are dead and there's no more messages:
            while (m != null || stream(producers).anyMatch(Thread::isAlive));
        });
        
        stream(producers).forEach(Thread::start);
        stream(consumers).forEach(Thread::start);
        
        concat(stream(producers), stream(consumers)).forEach(t -> {
            try {
                t.join(TimeUnit.MINUTES.toMillis(1));
            }
            catch (InterruptedException e) {
                fail(e.getMessage(), e);
            }
        });
        
        assertEquals(freq.size(), MESSAGES_PER_THREAD * THREADS);
        
        Map<Long, Long> deliveryCountToMsgCount = freq.values().stream()
                .collect(groupingBy(LongAdder::sum, counting()));
        
        System.out.println("Delivery count = Message count (most should be 1)");
        
        deliveryCountToMsgCount.entrySet().stream()
                .sorted(Comparator.comparingLong(Map.Entry::getKey))
                .forEach(e -> System.out.println("    " + e));
    }
    
    
    
    /**
     * Construct testee with a given timeout in seconds.
     * 
     * @param timeoutInSec
     *            timeout in seconds (use {@code -1} for implementation default)
     */
    private void given_timeout(int timeoutInSec) {
        testee = getFactory().apply(Duration.ofSeconds(timeoutInSec));
    }
    
    /**
     * Asserts all specified {@code messages} as the only content of the
     * specified {@code queue}.
     * 
     * The implementation will do one poll for each specified message and assert
     * equality. Once all messages has been asserted, we also assert that the
     * queue is empty.
     * 
     * @param queue     queue to poll messages from
     * @param messages  messages to assert
     */
    private void assert_queue_content(String queue, String... messages) {
        stream(messages).forEach(expected ->
                assertEquals(testee.poll(queue).get(), expected));
        
        assertNull(testee.poll(queue));
    }
    
    private static String[] mkStrings(int n, String prefix) {
        return IntStream.range(0, n)
                .mapToObj(i -> prefix + i)
                .toArray(String[]::new);
    }
    
    private static Thread[] mkThreads(int n, Runnable job) {
        Thread[] threads = new Thread[n];
        
        for (int i = 0; i < threads.length; ++i) {
            Thread t = new Thread(job);
            t.setDaemon(true);
            threads[i] = t;
        }
        
        return threads;
    }
    
    private static int rand(int endExclusive) {
        return ThreadLocalRandom.current().nextInt(0, endExclusive);
    }
    
    private static <E> E rand(E[] arr) {
        return arr[rand(arr.length)];
    }
}