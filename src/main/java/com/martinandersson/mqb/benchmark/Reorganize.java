package com.martinandersson.mqb.benchmark;

import java.io.IOException;
import static java.lang.Double.parseDouble;
import java.math.RoundingMode;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import static java.util.Comparator.comparing;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.NavigableSet;
import java.util.TreeMap;
import java.util.TreeSet;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toCollection;
import static java.util.stream.Collectors.toList;

/**
 * Take JMH:s summary from an output/results file and prettify it.<p>
 * 
 * This class was written only as a quick and dirty (non-extensible,
 * non-sensible, massive pain-in-the-ass script) yet useful utility for the
 * author instead of having to "group" and sort {@code QueueServiceBenchmark}
 * throughput results by hand in a text editor. You should probably not use it
 * for anything else?<p>
 * 
 * What it does is to group throughput summary records in the output file by
 * queue size, then by benchmark name, finally sort the scores descendingly and
 * add a trailing column to show the percentage gain compared to the previous
 * record.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class Reorganize
{
    private static final String FILE = System.getProperty("f");
    
    private static final int
            BENCHMARK  = 0,
            QUEUE_SIZE = 2,
            SCORE      = 5;
    
    
    
    public static void main(String... ignored) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(FILE), ISO_8859_1);
        
        Iterator<String> it = lines.iterator();
        
        String header = null;
        
        
        // Keep only the JMH summary lines (skip details)..
        while (it.hasNext()) {
            String l = it.next();
            
            it.remove();
            
            if (l.startsWith("Benchmark")) {
                header = l;
                break;
            }
        }
        
        
        // Group by 1) Queue size, 2) Benchmark.
        // Sort by descending score.
        // Add winner percentage gain compared to the previous lower ranked QS implementation.
        
        final List<String> out = new ArrayList<>();
        
        out.add(header);
        
        Map<String, List<String>> byQueue = lines.stream()
                // Only interested in throughput
                .filter(l -> l.contains("thrpt"))
                // Not interested in gotMessage- and gotNull subresults
                .filter(l -> !l.matches(".*\\b(gotMessage|gotNull)\\b.*"))
                .collect(groupingBy(l -> c(l, QUEUE_SIZE), TreeMap::new, toList()));
        
        byQueue.forEach((q, recordsInQueue) -> {
            Map<String, NavigableSet<String>> byBenchmark = recordsInQueue.stream().collect(groupingBy(
                    l -> c(l, BENCHMARK),
                    LinkedHashMap::new,
                    toCollection(() -> new TreeSet<>(comparing(l -> c((String) l, SCORE)).reversed()))));
            
            byBenchmark.entrySet().stream()
                    .map(Map.Entry::getValue)
                    .forEach(recordsInBenchmark -> {
                        for (String prev : recordsInBenchmark) {
                            String next = recordsInBenchmark.higher(prev);
                            
                            // Only compute difference if we have a next record
                            // to compare with (the loosing implementation)!
                            out.add(next == null ? prev :
                                    prev + "  +" + diff(next, prev));
                        }
                    });
            
            // Aka. newline after each queue size.
            out.add("");
        });
        
        
        // Dump to new file tagged "_summary"..
        
        final int dot = FILE.lastIndexOf('.');
        
        String outFile = (dot == -1 ? FILE : FILE.substring(0, dot))
                + "_summary";
        
        String ext = dot == -1 ? "" : FILE.substring(dot);
        
        Path file = Paths.get(FILE).resolveSibling(outFile + ext);
        
        Files.write(file, out, ISO_8859_1);
        
        System.out.printf("Dumped %s prettified record(s) to  >  %s%n",
                out.size(), file.toAbsolutePath());
    }
    
    private static final Map<String, String[]> COLUMNS = new HashMap<>();
    
    /**
     * Returns the column value of the specified {@code record}.
     * 
     * @param record  input string
     * @param index   column index
     * 
     * @return column value
     */
    private static String c(String record, int index) {
        return COLUMNS.computeIfAbsent(record, r -> r.split("\\s{2,}+"))[index];
    }
    
    private static String diff(String from, String to) {
        // Don't parse the error margin..
        return diff(parseDouble(c(from, SCORE).split("\\s", 2)[0]),
                    parseDouble(c(to,   SCORE).split("\\s", 2)[0]));
    }
    
    private static String diff(double from, double to) {
        if (from == 0) {
            throw new UnsupportedOperationException();
        }
        
        // Thanx: http://math.stackexchange.com/questions/716767/#comment-1945364
        return humanize((to - from) / (from < 0. ? from * -1. : from));
    }
    
    private static final ThreadLocal<NumberFormat> PERCENT_FORMATTER
            = ThreadLocal.withInitial(() -> {
                NumberFormat nf = DecimalFormat.getPercentInstance();
                
                nf.setMaximumFractionDigits(2);
                
                // Default is RoundingMode.HALF_EVEN. We do:
                nf.setRoundingMode(RoundingMode.HALF_UP);
                
                return nf;
            });
    
    /**
     * Convert a percent factor such as "0.05" to a human-readable String such
     * as "5%"<p>
     *
     * The exact result is dependent on the system's default locale. And with
     * that said, this method is primarily intended for printing percentages to
     * screen.<p>
     *
     * If necessary, the percentage will be rounded to two decimals.<p>
     *
     * The used {@code RoundingMode.HALF_UP} is the one "taught at school" to
     * quote the JavaDoc. This will produce the most accurate result for
     * rounding of the specified number, and will produce a result recognizable
     * by a human. However, for other more mathematical needs, then consider
     * using {@code RoundingMode.HALF_EVEN} which is..
     * <pre>
     *
     *   ..the rounding mode that statistically minimizes cumulative error when
     *   applied repeatedly over a sequence of calculations.
     * </pre>
     *
     * @param factor percent factor
     *
     * @return a human-readable String
     */
    public static String humanize(double factor) {
        return PERCENT_FORMATTER.get().format(factor);
    }
}