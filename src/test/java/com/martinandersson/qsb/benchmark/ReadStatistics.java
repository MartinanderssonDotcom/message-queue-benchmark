package com.martinandersson.qsb.benchmark;

import static java.lang.Math.addExact;
import org.openjdk.jmh.annotations.AuxCounters;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.State;

/**
 * For the reader of a queue, we branch the operation result into non-null
 * messages polled versus null messages polled. JMH will present these two
 * separately in the results together with the aggregated total.<p>
 * 
 * When the reader poll a null message, then that is because he outperform
 * the writer(s) or the {@code Queue} implementation hasn't [yet] made the
 * message(s) visible.<p>
 * 
 * In multi-threaded benchmark setups, I expect read-misses to be present versus
 * non-existent for serialized single-threaded benchmarks.<p>
 * 
 * For whatever reason, this branching is not picked up by JMH if performed in
 * single shot benchmarks. You may ask for a parameter of this type and even use
 * it, but it will have no effect.
 */
@State(Scope.Thread)
@AuxCounters(AuxCounters.Type.OPERATIONS)
public class ReadStatistics {
    public long gotNull,
                gotMessage;

    <M> M report(M message) {
        if (message == null) {
            gotNull = addExact(gotNull, 1);
        }
        else {
            gotMessage = addExact(gotMessage, 1);
        }

        return message;
    }
}