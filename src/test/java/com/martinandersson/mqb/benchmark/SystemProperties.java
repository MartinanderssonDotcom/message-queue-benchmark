package com.martinandersson.mqb.benchmark;

import java.text.MessageFormat;
import static java.util.Objects.requireNonNull;
import java.util.function.Consumer;

/**
 * Enumerated system properties.<p>
 * 
 * All of these properties are used by {@code StartJmh} as a facade for JMH
 * parameters.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public enum SystemProperties
{
    /**
     * A regex that specify which benchmarks to run.<p>
     * 
     * The property key is "r" and the property is optional. The value is a
     * regex passed to JMH as-is (except for one case, see next paragraph). If a
     * regex is not provided, all benchmarks will run and the benchmark class
     * file names are assumed to end with the name "Benchmark".<p>
     * 
     * I am not sure what exactly the regex is being matched
     * against<sup>1</sup>. JMH has very limited documentation.<p>
     * 
     * If the "regex" is made of all uppercase alphabetic characters, then the
     * String will be rebuilt to a regex that will match all benchmarks in class
     * names that has each letter in it.<p>
     * 
     * This yield a practical shorthand for how to refer to a specific benchmark
     * class. For example, "QSB" will run
     * <strong>Q</strong>ueue<strong>S</strong>ervice<strong>B</strong>enchmark
     * and nothing else.<p>
     * 
     * To be perfectly clear, "QSB" will be transformed to
     * "\\.Q\\p{Lower}+S\\p{Lower}+B\\p{Lower}+\\.".
     * 
     * <h3>Notes</h3>
     * It's probably class qualified name + "." + method. See implementation of
     * method {@code getUsername()}:
     * <pre>
     *   http://hg.openjdk.java.net/code-tools/jmh/file/6eb89dc11810/jmh-core/src/main/java/org/openjdk/jmh/runner/BenchmarkListEntry.java#l240
     * </pre>
     */
    BENCHMARK_REGEX ("r", "benchmark regex"),
    
    /**
     * Redirects the log output of JMH from console to a file.<p>
     * 
     * The property key is "lf" and the property is optional.
     */
    LOG_FILE ("lf", "log file"),
    
    /**
     * Will export the results to a comma-separated file.<p>
     * 
     * The property key is "rf" and the property is optional.
     */
    RESULT_FILE ("rf", "result file"),
    
    /**
     * Thread counts for grouped benchmark methods.<p>
     * 
     * The property key is "tg" and the property is optional.<p>
     * 
     * TODO: Value example.<p>
     * 
     * Specifying this property for benchmarks that has no benchmark "groups"
     * will totally crash with something similar to this:
     * <pre>
     *   java.lang.IllegalStateException: Harness failed to distribute threads among groups properly
     * </pre>
     * 
     * Understanding how to specify number of threads to run for JMH benchmarks
     * is a true pain in the ass. This author hiked through the valley of death
     * and wishes to express an experience-based opinion about what I perceive
     * to be the truth.<p>
     * 
     * {@code @Threads} or {@code ChainedOptionsBuilder.threads(int)} is the
     * malicious foundation for how to - kind of - specify number of threads in
     * JMH.<p>
     * 
     * If your benchmark method is "symmetric", i.e., it is not annotated with
     * {@code @Group}, then the specified thread count is exactly that - a
     * thread count. For example, value 2, make the benchmark method be called
     * by two threads. So far so good!<p>
     * 
     * But, the thread count parameter is absurdly wicked and inconsistent for
     * "asymmetric" - {@code @Group} annotated - benchmark methods.<p>
     * 
     * First of all, forget {@code @Threads}. For grouped benchmark methods,
     * number of threads is "supposed" to be specified by {@code @GroupThreads}
     * or the word-inverted {@code ChainOptionsBuilder.threadGroups(int...)}
     * which is <u>exactly the parameter that this system property is a facade
     * for</u>.<p>
     * 
     * Basically, "thread groups" is supposed to specify number of threads per
     * benchmark method that participate in the benchmark group. For example,
     * assume we have 3 grouped benchmark methods. Given this int array:
     * <pre>
     *   [1, 2, 3]
     * </pre>
     * 
     * ..then our three methods will be called with 1, 2 and 3 threads
     * respectively for a total of 1 + 2 + 3 = 6 threads.<p>
     * 
     * Of course, you could have specified this:
     * <pre>
     *   [1, 2]
     * </pre>
     * 
     * ..in which case JMH would silently forget all about the third benchmark
     * method and only call the first two with 1 and 2 threads respectively
     * (!).<p>
     * 
     * Specifying too many "groups" will crash. For example, {@code [1, 2, 3,
     * 4]} in our example would throw this exception (not to be mistaken for
     * the first exception listed on top of this JavaDoc when there are
     * <i>no</i> grouped methods!):
     * <pre>
     *   java.lang.IndexOutOfBoundsException: Index: 4, Size: 4
     * </pre>
     * 
     * Clearly, JMH seems to have lost his mind a long time ago. But it gets
     * even better. Let's rewind to 3 grouped methods with 3 specified ints. For
     * simplicity, assume these ints: {@code [1, 1, 1]}. That is, we use 3
     * threads in total.<p>
     * 
     * If you also set the {@code @Threads} parameter to 1, 2 or 3 - then it has
     * no effect. If you set the {@code @Threads} parameter to 4, then all of a
     * sudden JMH reacts with storm and thunder. 4 is more then the total count
     * of threads implicitly specified through the oh so awkwardly named "thread
     * groups" parameter. So JMH will spawn a second actual/real thread group.
     * Each of the two thread groups will use 1-1-1 threads for a total of 6
     * threads hammering our methods (not 4!).<p>
     * 
     * Continuing to increase the "thread count" value has no effect until we
     * reach {@code @Threads} = 7 which will use 3 threads groups for a total of
     * 9 threads, and so on.<p>
     * 
     * In the context of grouped benchmark methods, we can say that the
     * JMH parameter "thread groups" <i>is</i> the thread count and the
     * "threads" parameter is sort of a mix between thread count and number of
     * thread groups; expressing our "wish" of a minimum count of threads to use.
     * An overflow will cause JMH to use more simultaneous - real - thread
     * groups with the distribution specified through "thread groups". It is by
     * far the most weirdest, unintuitive and inconsistent thing this earth has
     * ever experience since the introduction day of toilet paper.<p>
     * 
     * Thankfully, {@code StartJmh} has no facade for the "thread count"
     * parameter. The thread count parameter defaults to 1, and the "thread
     * groups" will therefore be an actual thread count for the grouped
     * benchmark methods (unless you annotate your class/method differently). Of
     * course, your benchmark harness is still subject to the errors listed
     * above if you specify too few or too many ints lol.<p>
     * 
     * You might ask what is the order of my "thread groups"? If I specify 1-2,
     * or 2-1, which method get which count of threads? Jupp, totally
     * unspecified. But hopefully deterministic and should therefore be
     * addressed by JavaDoc of the benchmark class file.<p>
     * 
     * Finally, this property will hopefully be deprecated in the future:
     * <a href="https://bugs.openjdk.java.net/browse/CODETOOLS-7901012">
     *   https://bugs.openjdk.java.net/browse/CODETOOLS-7901012
     * </a>
     */
    THREAD_GROUPS ("tg", "thread groups");
    
    
    
    private final String
            prop,
            description;
    
    
    private SystemProperties(String prop, String description) {
        this.prop = prop;
        this.description = description;
    }
    
    
    /**
     * Returns the value of this system property, or {@code null} if it has not
     * been set.
     * 
     * @return the value of this system property, or {@code null} if it has not
     * been set
     */
    public String get() {
        return System.getProperty(prop);
    }
    
    /**
     * Returns the value of this system property.
     * 
     * @return the value of this system property
     * 
     * @throws NullPointerException  if the system property has not been set
     */
    public String require() {
        return requireNonNull(get(), () -> MessageFormat.format(
                "Failed to lookup {0}.", description));
    }
    
    /**
     * Returns {@code true} if this system property is present, otherwise {@code
     * false}.
     * 
     * @return {@code true} if this system property is present, otherwise {@code
     * false}
     */
    public boolean isPresent() {
        return get() != null;
    }
    
    /**
     * If value is present, apply the specified {@code consumer}.
     * 
     * @param consumer  property consumer
     * 
     * @return {@code true} if value is present, otherwise {@code false}
     */
    public boolean ifPresent(Consumer<String> consumer) {
        final String val = get();
        
        if (val == null) {
            return false;
        }
        
        consumer.accept(val);
        return true;
    }
}