package com.martinandersson.mqb.benchmark;

import java.nio.file.Paths;
import static java.util.Arrays.stream;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.joining;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.ChainedOptionsBuilder;
import org.openjdk.jmh.runner.options.OptionsBuilder;

/**
 * Application entry point for JMH benchmarks.<p>
 * 
 * Some JMH parameters may be set using {@linkplain SystemProperties system
 * properties}.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class StartJmh
{
    public static void main(String... ignored)
            throws RunnerException, ClassNotFoundException
    {
        // TODO: ResultFormatType
        // TODO: VerboseMode
        
        ChainedOptionsBuilder b = new OptionsBuilder()
                .include(getRegex())
                .forks(1)
                .jvmArgsAppend("-ea");
        
        SystemProperties.BENCHMARK_FILE.ifPresent(f -> {
            b.output(f);
            
            System.out.println("Writing results to file: " +
                    Paths.get(f).toAbsolutePath());
        });
        
        SystemProperties.THREAD_GROUPS.ifPresent(tg -> b.threadGroups(
                        stream(tg.split("-")).mapToInt(Integer::parseInt).toArray()));
        
        System.out.println("TH INPUT: " + SystemProperties.THREAD_GROUPS.get());
        
        new Runner(b.build()).run();
    }
    
    private static String getRegex() {
        String regex = SystemProperties.BENCHMARK_REGEX.get();
        
        if (regex == null) {
            regex = ".+Benchmark";
        }
        else if (Pattern.matches("\\p{Upper}+", regex)) {
            regex = regex.chars()
                    .mapToObj(x -> String.valueOf((char) x))
                    .collect(joining("\\p{Lower}+", "\\.", "\\p{Lower}+\\."));
        }
        
        return regex;
    }
}