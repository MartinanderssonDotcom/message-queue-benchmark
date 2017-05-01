package com.martinandersson.mqb.benchmark;

import static java.util.Arrays.stream;
import static java.util.Collections.unmodifiableMap;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.function.Predicate;
import static java.util.stream.Collectors.joining;
import java.util.stream.Stream;

/**
 * A row from JMH:s result file.
 * 
 * @author Martin Andersson (webmaster at martinandersson.com)
 */
class Row
{
    static final String DELIMITER = ",";
    
    
    private final Map<String, String> columnToValues;
    
    
    
    static Row header(String input) {
        return new Row(null, input);
    }
    
    static Row data(Row header, String input) {
        return new Row(header, input);
    }
    
    
    
    private Row(Row header, String input) {
        Map<String, String> values = new LinkedHashMap<>();
        
        String[] parts = input.split(DELIMITER);
        
        if (header == null) {
            stream(parts).map(this::unquote)
                    .forEach(lbl -> values.put(lbl, null));
        }
        else {
            Iterator<String> lbl = header.columnToValues.keySet().iterator(),
                             prt = stream(parts).iterator();
            
            while (lbl.hasNext()) {
                values.put(lbl.next(), unquote(prt.next()));
            }
            
            if (prt.hasNext()) {
                throw new IllegalArgumentException();
            }
        }
        
        columnToValues = unmodifiableMap(values);
    }
    
    private String unquote(String str) {
        if (str.isEmpty()) {
            return str;
        }
        
        if (str.startsWith("\"")) {
            str = str.substring(1);
        }
        
        if (str.endsWith("\"")) {
            str = str.substring(0, str.length() - 1);
        }
        
        return str;
    }
    
    
    
    String value(String label) {
        return columnToValues.get(label);
    }
    
    String mkString() {
        return mkString(null, null, null);
    }
    
    String mkString(
            Predicate<String> lblFilter,
            Comparator<String> lblOrder,
            BiFunction<String, String, String> valueMapper)
    {
        Stream<Map.Entry<String, String>> entries
                = columnToValues.entrySet().stream();
        
        if (lblFilter != null) {
            entries = entries.filter(e -> lblFilter.test(e.getKey()));
        }
        
        if (lblOrder != null) {
            entries = entries.sorted((a, b) ->
                    lblOrder.compare(a.getKey(), b.getKey()));
        }
        
        return entries.map(valueMapper == null ?
                  Map.Entry::getValue :
                  e -> valueMapper.apply(e.getKey(), e.getValue()))
                .collect(joining(DELIMITER));
    }
    
    
    
    /**
     * {@inheritDoc}
     */
    @Override
    public String toString() {
        return columnToValues.toString();
    }
}