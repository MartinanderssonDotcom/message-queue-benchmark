package com.martinandersson.mqb.benchmark;

import java.io.IOException;
import static java.lang.Double.parseDouble;
import static java.lang.Integer.compare;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.DecimalFormat;
import java.text.NumberFormat;
import java.util.ArrayList;
import static java.util.Arrays.stream;
import java.util.Comparator;
import java.util.List;
import static java.util.stream.Collectors.joining;

/**
 * Take JMH:s summary from a results file and reorganize it.<p>
 * 
 * This class was written only as a quick and dirty (non-extensible,
 * non-sensible, massive pain-in-the-ass script) yet useful utility for the
 * author instead of having to sort {@code QueueServiceBenchmark} throughput
 * results by hand in a text editor. You should probably not use it for anything
 * else?<p>
 * 
 * What it does is to sort throughput summary records in the result file by
 * queue size, benchmark name and descending scores. Also we'll slap on a cute
 * column to show the percentage gain comparing each row to the trailing loser.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class Reorganize
{
    private static final String
            FILE        = System.getProperty("rf"),
            BENCHMARK   = "Benchmark",
            SCORE       = "Score",
            SCORE_ERROR = "Score Error (99.9%)",
            UNIT        = "Unit",
            P_IMPL      = "Param: impl",
            P_QUEUES    = "Param: queues";
    
    private static final String[] KEEP_ORDERLY = {
            BENCHMARK, P_IMPL, P_QUEUES, SCORE, SCORE_ERROR, UNIT };
    
    
    
    public static void main(String... ignored) throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(FILE));
        List<Row>    data  = new ArrayList<>();
        
        Row header = null;
        
        for (String l : lines) {
            if (header == null) {
                header = Row.header(l);
            }
            else {
                Row r = Row.data(header, l);
                
                // Not interested in gotMessage- and gotNull subresults
                if (!r.value(BENCHMARK).matches(".*\\b(gotMessage|gotNull)\\b.*")) {
                    data.add(Row.data(header, l));
                }
            }
        }
        
        data.sort(Comparator.<Row, String>comparing(r ->
                r.value(P_QUEUES)).thenComparing(r ->
                r.value(BENCHMARK)).thenComparing(Comparator.<Row, Double>comparing(r ->
                parseScore(r)).reversed()));
        
        
        List<String> out = new ArrayList<>();
        
        // Add header and toss on an extra Gain column.
        out.add(stream(KEEP_ORDERLY).collect(joining(Row.DELIMITER)) + Row.DELIMITER + "Gain");
        
        for (int i = 0; i < data.size(); ++i) {
            Row row = data.get(i);
            
            String o = row.mkString(
                    // Keep all labels/columns as defined in KEEP_ORDERLY (discard the others).
                    lbl -> pos(lbl) != -1,
                    // Order them according to position in KEEP_ORDERLY.
                    (a, b) -> compare(pos(a), pos(b)),
                    // Replace abhorent benchmark names with leet names.
                    (lbl, val) -> {
                        String replace = val;
                        
                        if (lbl.equals(BENCHMARK)) {
                            switch (val.replaceFirst("com.martinandersson.mqb.benchmark.QueueServiceBenchmark.", "")) {
                                case "thrpt":
                                    replace = "Total";
                                    break;
                                case "thrpt:reader_thrpt":
                                    replace = "Reader";
                                    break;
                                case "thrpt:writer_thrpt":
                                    replace = "Writer";
                                    break;
                            }
                        }
                        return replace;
                    });
            
            // Add percentage gain if applicable (there's a row below in same
            // benchmark- and queue category).
            if (i < data.size() - 1) {
                Row next = data.get(i + 1);
                
                if (next.value(BENCHMARK).equals(row.value(BENCHMARK)) &&
                    next.value(P_QUEUES).equals(row.value(P_QUEUES)))
                {
                    o += Row.DELIMITER + diff(next, row);
                }
            }
            
            out.add(o);
        }
        
        
        // Dump to new file tagged "_reorg"..
        
        final int dot = FILE.lastIndexOf('.');
        
        String outFile = (dot == -1 ? FILE : FILE.substring(0, dot))
                + "_reorg";
        
        String ext = dot == -1 ? "" : FILE.substring(dot);
        
        Path file = Paths.get(FILE).resolveSibling(outFile + ext);
        
        Files.write(file, out);
        
        System.out.printf("Dumped %s reorganized record(s) to  >  %s%n",
                out.size() - 1, file.toAbsolutePath());
    }
    
    private static int pos(String label) {
        for (int i = 0; i < KEEP_ORDERLY.length; ++i) {
            if (KEEP_ORDERLY[i].equals(label)) {
                return i;
            }
        }
        
        return -1;
    }
    
    private static double parseScore(Row row) {
        try {
            return parseDouble(row.value(SCORE));
        }
        catch (NumberFormatException e) {
            NumberFormatException e0 = new NumberFormatException(
                    e.getMessage() + ", Row: " + row);

            e0.addSuppressed(e);
            throw e0;
        }
    }
    
    private static String diff(Row from, Row to) {
        return diff(parseScore(from), parseScore(to));
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
                
                nf.setGroupingUsed(false);
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