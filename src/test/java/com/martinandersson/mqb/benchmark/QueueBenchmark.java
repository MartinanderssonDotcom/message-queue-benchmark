package com.martinandersson.mqb.benchmark;

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
        
        /*
         * For parallelized queue benchmarks, it's not straight forward how
         * measurements on throughput and average shall be interpreted.
         * 
         * There is no guarantee that our two reader and writer threads will
         * operate at the same speed. If the writer thread is faster than the
         * reader, then internal capacity/storage of the queue will grow over
         * time and this growth will most likely impact performance.
         * 
         * In other words, if the writer is faster, and we have a time based
         * trial run (throughput and average) then we could end up having a
         * situation where a highly performant queue implementation must
         * accomodate more growth and thus be "penalized" compared to other
         * queue implementations. Even if all queue implementions has the
         * exact same runtime performance and existent growth cost, then still
         * the benchmark itself would yield a different result simply by having
         * it execute longer (growth cost will be larger). Usually, we would
         * like a longer observed time period to increase our confidence but we
         * would be a bit surprised if we find that it drastically change the
         * performance metrics!
         * 
         * This particular trait caused JMH to declare the benchmark mode
         * "single shot" as "the only acceptable benchmark mode" for benchmark
         * code that doesn't have a steady state. See:
         *     http://hg.openjdk.java.net/code-tools/jmh/file/5c8f74626ab2/jmh-samples/src/main/java/org/openjdk/jmh/samples/JMHSample_26_BatchSize.java#l59
         * 
         * I see the problem, but I do not agree that single shot is "the only
         * acceptable" solution. In fact, I argue that single shot does not
         * solve the problem. What single shot do is to repackage the problem.
         * First, let's toss some light on wtf single shot is.
         * 
         * Throughput (operations per time unit) and average (time units per
         * operation) run a benchmark method over and over again for a given
         * time period (default is 1 second). One such time-based run equals one
         * iteration. But, we don't like time-based runs for benchmark code that
         * does not have a steady-state.
         * 
         * Single shot will produce time units per operation, but function a bit
         * differently. Single shot call the benchmark method only 1 single time
         * and consider that as one iteration.
         * 
         * For example, throughput run a benchmark 10 times, 1 second each. Now
         * JMH can say that the result is blabla operations (or rather, method
         * calls) per second. With single shot, JMH call the method 10 times.
         * Period. 10 "cold" invocations is our entire scientific base ("cold"
         * is derived from the fact that JMH fork a new JVM process for each
         * trial to make it harder for code profiles to be shared). If these 10
         * calls took 10 seconds, JMH could say our benchmark performance is 1
         * operation per second.
         * 
         * Single shot is not a time-based run but will usually yield far too
         * few observations. Plus the benchmark workload is oftentimes quite
         * small. If this workload has a very unstable state, then a small
         * workload will probably make the measurements even more disperse and
         * schizofrenic, further increasing the pressure on us to bump the
         * iteration count.
         * 
         * One alternative is to increase the workload by looping the method
         * implementation. Instead of saying one operation/observation is "write
         * and read 1 message", I could say "write and read 10 000 messages".
         * In order to protect us from loop unrolling, JMH provide us with the
         * "batch size" parameter as a way to leave the looping up to JMH. If I
         * say that the batch size is 10 000, then JMH will call the method not
         * only 1 time, but 10 000 times (maybe "loop" would have been a better
         * annotation member name, or even better, another annotation). If I, as
         * it is in our case below, group two methods into one benchmark, then I
         * must annotate both methods with the same batch size for a total count
         * of method calls equal to two times the batch size.
         * 
         * However, JMH still consider all 10 000 calls as only one
         * operation/observation. The benchmark result is not divided or
         * otherwise manipulated just because JMH know that we artificially
         * increased our workload!
         * 
         * For each looped benchmark method call, the queue implementation
         * instance is not replaced. If for whatever reason the writer is
         * extremely fast and the reader is extremely slow, then the queue
         * implementation may be required to accommodate growth enough to cater
         * for elements equal to the the total count of method calls. The
         * benefit we get is that the growth is now bounded by batch size
         * instead of being unbounded.
         * 
         * Running a benchmark with no steady state in average mode, would
         * enable us to state the research question something like this:
         * 
         *   Given N number of 1 second runs with an unbounded element-growth
         *   potential, how much on average did one workload cost?
         * 
         * The "improvement" single shot give us is the ability to rebrand the
         * question:
         * 
         *   Given N number of method calls with a bounded element-growth
         *   potential equal to the batch size, how much on average did one
         *   workload cost?
         * 
         * The only difference here is the first part, what we state about our
         * environment/setup - given this or given that. But we do not magically
         * get rid of non-steady state by switching mode. Sure, with single
         * shot, we remove the time-based parameter, but on the other hand we
         * introduce another one; the batch size. Generically speaking, it is
         * more advantageous to have something more specific than to have
         * something less specific, but the growth dilemma is still very much
         * present.
         * 
         * At the end of the day, I make the argument that the first research
         * question is "just as valid" as the second one. Maybe the benchmark
         * author want to benchmark a time-based environment - for whatever
         * reason. Maybe the benchmark author want to see which implementation
         * can perform the best during N number of seconds because his
         * application intends to discard the instance after this time period?
         * Who knows! Also take into consideration that if the benchmark has no
         * steady state, then so too is the real-world application also missing
         * one. For things like message queues, it must be considered an anomaly
         * if a real-world queue always see an equal number of producers and
         * consumers that furthermore operate at the exact same speeds. So why
         * would we put effort into trying to code around reality? What I'm
         * trying to get at is that I can see how the first research question
         * could be even more valid than the second research question.
         * 
         * Going back to JMH:s own example, I think that maybe the "wrong"
         * benchmarks they wrote is either right on or convoluted. If one wanted
         * to see just how the performance changes over time and growth, then
         * certainly the "wrong" benchmarks are actually right on spot. If one
         * are more after trying to determine the lookup cost of the middle
         * index independent of growth, then the size of the list is a very
         * important factor we want both given and kept static (plus/minus 1) as
         * part of the experiment environment. If that requires us to launch and
         * compare many different benchmarks given many different sizes, then so
         * be it. We can only learn more!
         * 
         * I do understand they tried to make a point, and that they do well. I
         * just wish to take their point and further demonstrate that JMH:s
         * statement that single shot is the "only acceptable" mode may be
         * 1) completely invalid and 2) there might even be alternative
         * solutions to the "problem" than switching mode. In the end, it will
         * always be up to the benchmark author to define what is being
         * measured, and offer guidance on how to interpret the results.
         * 
         * Therefor, I present one benchmark below for each mode! The result
         * guidance should be provided in the JavaDoc at the top.
         */
        
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