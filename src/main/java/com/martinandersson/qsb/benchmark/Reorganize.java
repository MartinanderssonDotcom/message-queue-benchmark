package com.martinandersson.qsb.benchmark;

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
import java.util.Collection;
import java.util.Comparator;
import static java.util.Comparator.comparing;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import static java.util.stream.Collectors.groupingBy;
import static java.util.stream.Collectors.joining;
import static java.util.stream.Collectors.minBy;

/**
 * Reorganize a JMH results file and write to a new file specified with property
 * "rf".<p>
 * 
 * This class was written only as a quick and dirty (non-extensible,
 * non-sensible, massive pain-in-the-ass script) yet useful utility for the
 * author instead of having to sort {@code QueueServiceBenchmark} throughput
 * results by hand in a text editor. You should probably not use it for anything
 * else?<p>
 * 
 * What it does is to sort throughput summary records from the result file by
 * queue size, benchmark name and descending scores. Also we'll slap on a cute
 * column to show percentage gains comparing each row to 1) the trailing loser
 * and 2) the bottom looser.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
public class Reorganize
{
    private static final String
            FILE        = System.getProperty("rf"),
            BENCHMARK   = "Benchmark",
            MODE        = "Mode",
            SCORE       = "Score",
            SCORE_ERROR = "Score Error (99.9%)",
            UNIT        = "Unit",
            IMPL        = "Param: impl";
    
    private static final String[] KEEP_ORDERLY = {
            BENCHMARK, IMPL, SCORE, SCORE_ERROR, UNIT };
    
    
    
    public static void main(String... ignored) throws IOException {
        writeToFile(reorganize(readRows()));
    }
    
    private static List<Row> readRows() throws IOException {
        List<String> lines = Files.readAllLines(Paths.get(FILE));
        
        final List<Row> rows = new ArrayList<>();
        
        Row header = null;
        
        for (String l : lines) {
            if (header == null) {
                header = Row.header(l);
            }
            else {
                Row r = Row.data(header, l);
                
                // Only interested in throughput
                // Not interested in gotMessage- and gotNull subresults
                if (r.value(MODE).equals("thrpt") &&
                    !r.value(BENCHMARK).matches(".*\\b(gotMessage|gotNull)\\b.*"))
                {
                    rows.add(Row.data(header, l));
                }
            }
        }
        
        rows.sort(Comparator.<Row, String>comparing(r ->
                r.value(BENCHMARK)).thenComparing(Comparator.<Row, Double>comparing(r ->
                parseScore(r)).reversed()));
        
        return rows;
    }
    
    private static List<String> reorganize(List<Row> rows) {
        final List<String> out = new ArrayList<>();
        
        // Add header and toss on extra gain columns.
        out.add(stream(KEEP_ORDERLY).collect(joining(Row.DELIMITER))
                + Row.DELIMITER + "Rel. Gain" + Row.DELIMITER + "Acc. Gain");
        
        Map<String, Optional<Row>> loosers = rows.stream().collect(
                groupingBy(r -> r.value(BENCHMARK),
                minBy(comparing(r -> parseScore(r)))));
        
        for (int i = 0; i < rows.size(); ++i) {
            final Row $this = rows.get(i);
            
            String o = $this.mkString(
                    // Keep all labels/columns as defined in KEEP_ORDERLY (discard the others).
                    lbl -> pos(lbl) != -1,
                    // Order them according to position in KEEP_ORDERLY.
                    (a, b) -> compare(pos(a), pos(b)),
                    // Replace abhorent benchmark names with leet names.
                    (lbl, val) -> {
                        String replace = val;
                        
                        if (lbl.equals(BENCHMARK)) {
                            switch (val.replaceFirst("com.martinandersson.qsb.benchmark.QueueServiceBenchmark.Thrpt.", "")) {
                                case "":
                                    replace = "Total";
                                    break;
                                case ":reader":
                                    replace = "Reader";
                                    break;
                                case ":writer":
                                    replace = "Writer";
                                    break;
                            }
                        }
                        return replace;
                    });
            
            // Add relative percentage gain if applicable (= there's a row below in same benchmark).
            // Add accumulative percentage gain if applicable (= the looser is not this row)
            if (i < rows.size() - 1) {
                Row next = rows.get(i + 1);
                
                if (next.value(BENCHMARK).equals($this.value(BENCHMARK))) {
                    o += Row.DELIMITER + diff(next, $this);
                }
                
                Row looser;
                if ((looser = loosers.get($this.value(BENCHMARK)).get()) != $this) {
                    o += Row.DELIMITER + diff(looser, $this);
                }
            }
            
            out.add(o);
        }
        
        return out;
    }
    
    private static void writeToFile(Collection<? extends CharSequence> lines) throws IOException {
        // Dump to new file tagged "_reorg"..
        
        final int dot = FILE.lastIndexOf('.');
        
        String outFile = (dot == -1 ? FILE : FILE.substring(0, dot))
                + "_reorg";
        
        String ext = dot == -1 ? "" : FILE.substring(dot);
        
        Path file = Paths.get(FILE).resolveSibling(outFile + ext);
        
        Files.write(file, lines);
        
        System.out.printf("Dumped %s reorganized record(s) to  >  %s%n",
                lines.size() - 1, file.toAbsolutePath());
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