package com.martinandersson.mqb.benchmark;

import java.nio.file.Paths;
import static java.util.Arrays.stream;
import java.util.regex.Pattern;
import static java.util.stream.Collectors.joining;
import org.openjdk.jmh.results.format.ResultFormatType;
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
        // TODO: VerboseMode
        
        ChainedOptionsBuilder b = new OptionsBuilder()
                .include(getRegex())
                .shouldDoGC(true)
                .jvmArgsAppend("-ea");
        
        SystemProperties.LOG_FILE.ifPresent(lf -> {
            b.output(lf);
            
            System.out.println("Logging to file  >  " + absolute(lf));
        });
        
        SystemProperties.RESULT_FILE.ifPresent(rf -> {
            b.result(rf);
            b.resultFormat(ResultFormatType.CSV);
            
            System.out.println("Writing results to file  >  " + absolute(rf));
        });
        
        SystemProperties.THREAD_GROUPS.ifPresent(tg -> b.threadGroups(
                stream(tg.split("-")).mapToInt(str -> {
                    // TODO: Update JavaDoc of THREAD_GROUPS!
                    final char last = str.charAt(str.length() - 1);
                    
                    if (last == 'x' || last == 'X') {
                        return Integer.parseInt(str.substring(0, str.length() - 1)) *
                            Runtime.getRuntime().availableProcessors();
                    }
                    
                    return Integer.parseInt(str);
                }).toArray()));
        
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
    
    private static String absolute(String file) {
        return Paths.get(file).toAbsolutePath().toString();
    }
}