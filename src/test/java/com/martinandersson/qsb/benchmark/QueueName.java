package com.martinandersson.qsb.benchmark;

import static java.util.Arrays.stream;
import java.util.Iterator;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;
import java.util.function.Supplier;
import java.util.stream.IntStream;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

/**
 * 
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
@State(Scope.Thread)
public class QueueName implements Supplier<String>
{
    static final int QUEUES
            = SystemProperties.QUEUE_SIZE.getInt().orElseThrow(
                    () -> new IllegalArgumentException("Please specify a queue size."));
    
    
    
    private Iterator<String> name;

    @Setup
    public void setupThread() {
        int[] seq = shuffle(IntStream.rangeClosed(1, QUEUES).toArray());

        name = new FixedCostLoopingIterator<>(
                stream(seq).mapToObj(n -> "Q" + n));
    }

    private static int[] shuffle(int[] arr) {
        final Random rnd = ThreadLocalRandom.current();

        // Thank you: http://stackoverflow.com/a/1520212
        for (int i = arr.length - 1; i > 0; --i) {
            int index = rnd.nextInt(i + 1);

            // Swap
            int e = arr[index];
            arr[index] = arr[i];
            arr[i] = e;
        }

        return arr;
    }

    @Override
    public String get() {
        return name.next();
    }
}