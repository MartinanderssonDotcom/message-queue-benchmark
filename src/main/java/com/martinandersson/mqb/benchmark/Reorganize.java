package com.martinandersson.mqb.benchmark;

import java.io.IOException;
import static java.nio.charset.StandardCharsets.ISO_8859_1;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import static java.util.Comparator.comparing;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.toList;

/**
 * Take JMH:s summary from an output/results file and prettify it.<p>
 * 
 * This class was written only as a useful utility for the author instead of
 * "grouping" and sorting {@code QueueServiceBenchmark} throughput results
 * manually. You should probably not use it for anything else?<p>
 * 
 * What it does is to group throughput summary records in the output file by
 * queue size, then by benchmark name, and finally sort the scores descendingly.
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
        
        
        // Group by 1) Queue size, 2) Benchmark. Sort by descending score..
        
        final List<String> out = new ArrayList<>();
        
        out.add(header);
        
        Map<String, List<String>> byQueue = lines.stream()
                // Only interested in throughput
                .filter(l -> l.contains("thrpt"))
                // Not interested in gotMessage- and gotNull subresults
                .filter(l -> !l.matches(".*\\b(gotMessage|gotNull)\\b.*"))
                .collect(groupingBy(l -> c(l, QUEUE_SIZE), TreeMap::new, toList()));
        
        byQueue.forEach((q, records) -> {
            Map<String, List<String>> byBenchmark = records.stream().collect(
                    groupingBy(l -> c(l, BENCHMARK), LinkedHashMap::new, toList()));
            
            byBenchmark.entrySet().stream().forEach(e -> e.getValue().stream()
                    .sorted(comparing(l -> c((String) l, SCORE)).reversed())
                    .forEach(out::add));
            
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
}