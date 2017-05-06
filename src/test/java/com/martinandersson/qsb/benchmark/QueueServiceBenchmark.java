package com.martinandersson.qsb.benchmark;

import com.martinandersson.qsb.api.Message;
import com.martinandersson.qsb.api.QueueService;
import static java.lang.System.out;
import static java.util.concurrent.TimeUnit.MICROSECONDS;
import static java.util.stream.Collectors.joining;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Fork;
import org.openjdk.jmh.annotations.Group;
import org.openjdk.jmh.annotations.Level;
import org.openjdk.jmh.annotations.Measurement;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;
import org.openjdk.jmh.annotations.TearDown;
import org.openjdk.jmh.annotations.Warmup;
import org.openjdk.jmh.infra.BenchmarkParams;
import org.openjdk.jmh.infra.ThreadParams;

/**
 * Queue service benchmarks.<p>
 * 
 * All benchmark methods herein are "asymmetric" with one workload for reader
 * threads and one workload for writer threads. Number of readers/writers are
 * specified using a system property "tg". For example, "1-2" will yield 1 reader
 * thread and 1 writer thread.<p>
 * 
 * We also need to know the queue size. Use system property "q" to specify that.
 * 
 * @author Martin Anderson (webmaster at martinandersson.com)
 * 
 * @see SystemProperties#THREAD_GROUPS
 * @see SystemProperties#QUEUE_SIZE
 */
// TODO: Production values!
@Fork(10)
@Warmup(iterations = 20)
@Measurement(iterations = 20, time = 1)
@OutputTimeUnit(MICROSECONDS)
@State(Scope.Benchmark)
public abstract class QueueServiceBenchmark
{
    // Iteration- and batch sizes for single shot benchmarks.
    // -----
    
    private static final int SS_ITERATIONS = 100,
                             SS_BATCH_SIZE = 100_000;
    
    
    
    // For each trial, we need a queue service to benchmark.
    // (number of queues is read by class QueueName)
    // -----
    
    QueueService qs;
    
    
    
    // This is the work we do. We write and read.
    // ------
    
    protected final void write(String queue, String message) {
        qs.push(queue, message);
    }
    
    protected final Message read(String queue) {
        final Message m = qs.pull(queue);
        
        if (m != null) {
            qs.complete(m);
        }
        
        return m;
    }
    
    protected final Message read(String queue, ReadStatistics rs) {
        return rs.report(read(queue));
    }
    
    
    
    // Setup
    // -----
    
    @Param
    QSImpl impl;
    
    @Setup
    public void setupTrial(BenchmarkParams bParams, ThreadParams tParams) {
        if (!SystemProperties.LOG_FILE.isPresent()) {
            return;
        }
        
        // Console risk being mute for a very long time if we log to file,
        // so be kind and dump benchmark details.
        
        out.println();
        out.println(    "Running " + bParams.getBenchmark());
        String params = "        Implementation " + impl + ", " + QueueName.QUEUES + " queue(s)";
        
        int[] groups = bParams.getThreadGroups();
        
        if (groups.length != 2) {
            throw new IllegalArgumentException("Bad \"thread group\" parameter: " +
                    IntStream.of(groups).mapToObj(Integer::toString).collect(joining(",", "[", "]")));
        }
        
        params += ", " + groups[0] + " reader(s) and " + groups[1] + " writer(s) in " + tParams.getGroupCount() + " group(s)";
        
        out.println(params);
    }
    
    @Setup(Level.Iteration)
    public void setupIteration() {
        qs = impl.get();
    }
    
    @TearDown(Level.Iteration)
    public void tearDownIteration() {
        qs = null;
    }
    
    
    
    // Benchmarks
    // -----
    
    @BenchmarkMode(Mode.Throughput)
    public static class Thrpt extends QueueServiceBenchmark {
        @Group("")
        @Benchmark
        public void writer(QueueName queue, QueueMessage message) {
            write(queue.get(), message.msg);
        }
        
        @Group("")
        @Benchmark
        public Message reader(QueueName queue, ReadStatistics rs) {
            return read(queue.get(), rs);
        }
    }
    
    @BenchmarkMode(Mode.AverageTime)
    public static class Avg extends QueueServiceBenchmark {
        @Group("")
        @Benchmark
        public void writer(QueueName queue, QueueMessage message) {
            write(queue.get(), message.msg);
        }
        
        @Group("")
        @Benchmark
        public Message reader(QueueName queue, ReadStatistics rs) {
            return read(queue.get(), rs);
        }
    }
    
    @BenchmarkMode(Mode.SingleShotTime)
    @Measurement(iterations = SS_ITERATIONS, batchSize = SS_BATCH_SIZE)
    public static class SingleShot extends QueueServiceBenchmark {
        @Group("")
        @Benchmark
        public void writer(QueueName queue, QueueMessage message) {
            write(queue.get(), message.msg);
        }
        
        @Group("")
        @Benchmark
        public Message reader(QueueName queue) {
            return read(queue.get());
        }
    }
}