package com.martinandersson.qsb.benchmark;

import java.util.ArrayDeque;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.LinkedTransferQueue;
import java.util.concurrent.PriorityBlockingQueue;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import java.util.function.Supplier;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.Warmup;

/**
 * This is toy code to benchmark a number of {@code Queue} implementations.<p>
 * 
 * For each implementation under test, we expose two types of benchmarks; one
 * that uses only 1 thread to first do a write and then read back same message.
 * Another benchmark type that is asymmetric/parallel; a reader and a writer
 * (default is 1 thread each).<p>
 * 
 * We expose all queues for the first type of serialized benchmarks, but only
 * thread-safe queues for the second type of parallelized benchmarks.<p>
 * 
 * Originally I let all implementations run through all benchmarks, but when not
 * thread-safe queues was exposed to multiple threads, all kinds of errors
 * happened. Normally, this is just fine (and expected!) because if an exception
 * is thrown during a benchmark invocation, JMH will simply log that shit and
 * remove the benchmark results from the summarization at the bottom.
 * 
 * In fact, exposing not thread safe implementations in a concurrent environment
 * made it very clear which queues are not thread-safe and that was kind of fun
 * to witness! However, one of the problems that hit me was a <strong>deadlock
 * </strong> lol.. which is totally unexpected since I would assume that queues
 * that is not thread-safe also has nothing to do with locks! And we can't have
 * benchmarks randomly freezing can we =) Plus we should only test "real-world"
 * code anyways.<p>
 * 
 * The separation of thread-safe queues versus not thread-safe queues coupled
 * with a desire to stay within one .java file is the reason for the current
 * design of having an abstract superclass with two nested concrete types.
 * Please holla at me if you find a better solution!<p>
 * 
 * Active JMH settings is only meant to provide a coarse-grained overview. I do
 * not think these settings are sufficient for a scientific study. If that is
 * your end goal, then consider bumping up number of iterations and so on.<p>
 * 
 * 
 * 
 * <h3>Results on author's machine</h3>
 * 
 * TODO: Blabla.
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@Warmup(iterations = 1) // TODO: 5
@Measurement(iterations = 1, time = 1) // TODO: 5, 3
@OutputTimeUnit(MICROSECONDS)
@State(Scope.Benchmark)
public abstract class QueueBenchmark
{
    // Iteration- and batch sizes for single shot benchmarks.
    // -----
    
    private static final int SS_ITERATIONS = 1, // TODO: 100
                             SS_BATCH_SIZE = 1; // TODO: 100_000
    
    
    
    // For each trial, we need a queue to benchmark..
    // ------
    
    Queue<Long> queue; // TODO: Make String or whatever. Has to be fixed cost to produce a message.
    
    @Setup
    public void setup() {
        queue = getQueue();
    }
    
    protected abstract Queue<Long> getQueue();
    
    
    
    // This is the work we do. We write and read..
    // ------
    
    protected final void write() {
        assert queue.add(Thread.currentThread().getId());
    }
    
    protected final Long read() {
        return queue.poll();
    }
    
    protected final Long read(ReadStatistics rs) {
        return rs.report(queue.poll());
    }
    
    
    
    // All queue implementations will run through a set of serialized benchmarks..
    // ------
    
    @Benchmark
    public long writeRead_thrpt(ReadStatistics rs) {
        write();
        return read(rs);
    }
    
    @Benchmark
    @BenchmarkMode(Mode.AverageTime)
    public long writeRead_avg(ReadStatistics rs) {
        write();
        return read(rs);
    }
    
    
    
    // Not thread-safe queue implementations..
    // ------
    
    public static class Serialized extends QueueBenchmark
    {
        public enum Impl implements Supplier<Queue<Long>> {
            ArrayDeque    (ArrayDeque::new),
            LinkedList    (LinkedList::new),
            PriorityQueue (PriorityQueue::new);
            
            private final Supplier<Queue<Long>> delegate;
            
            private Impl(Supplier<Queue<Long>> delegate) {
                this.delegate = delegate;
            }
            
            @Override
            public Queue<Long> get() {
                return delegate.get();
            }
        }
        
        @Param
        Impl implementation;
        
        @Override
        protected Queue<Long> getQueue() {
            return implementation.get();
        }
    }
    
    
    
    // Thread-safe queue implementations..
    // ------
    
    public static class Parallelized extends QueueBenchmark
    {
        public enum Impl implements Supplier<Queue<Long>> {
            LinkedBlockingQueue   (LinkedBlockingQueue::new),
            LinkedBlockingDeque   (LinkedBlockingDeque::new),
            LinkedTransferQueue   (LinkedTransferQueue::new),
            ConcurrentLinkedQueue (ConcurrentLinkedQueue::new),
            ConcurrentLinkedDeque (ConcurrentLinkedDeque::new),
            PriorityBlockingQueue (PriorityBlockingQueue::new);
            // TODO: Add our QueueService implementations!
            
            private final Supplier<Queue<Long>> delegate;
            
            private Impl(Supplier<Queue<Long>> delegate) {
                this.delegate = delegate;
            }
            
            @Override
            public Queue<Long> get() {
                return delegate.get();
            }
        }
        
        @Param
        Impl implementation;
        
        @Override
        protected Queue<Long> getQueue() {
            return implementation.get();
        }
        
        
        
        // Thread-safe specific benchmarks..
        // ------
        
        @Group("thrpt")
        @Benchmark
        public void writer_thrpt() {
            write();
        }
        
        @Group("thrpt")
        @Benchmark
        public Long reader_thrpt(ReadStatistics rs) {
            return read(rs);
        }
        
        @Group("avg")
        @Benchmark
        @BenchmarkMode(Mode.AverageTime)
        public void writer_avg() {
            write();
        }
        
        @Group("avg")
        @Benchmark
        @BenchmarkMode(Mode.AverageTime)
        public Long reader_avg(ReadStatistics rs) {
            return read(rs);
        }
        
        @Group("ss")
        @Benchmark
        @BenchmarkMode(Mode.SingleShotTime)
        @Measurement(iterations = SS_ITERATIONS, batchSize = SS_BATCH_SIZE)
        public void writer_ss() {
            write();
        }
        
        @Group("ss")
        @Benchmark
        @BenchmarkMode(Mode.SingleShotTime)
        @Measurement(iterations = SS_ITERATIONS, batchSize = SS_BATCH_SIZE)
        public Long reader_ss() {
            return read();
        }
    }
}