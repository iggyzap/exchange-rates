package models.iggy.zap;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Value object to represent tabulated currencies before rendering
 */
public class CsvTabulator {

    public Set<String> currencyNames;
    public SortedMap<Date, Map<String, Float>> groupedFx;

    public CsvTabulator(Set<String> currencyNames, SortedMap<Date, Map<String, Float>> groupedFx) {
        this.currencyNames = currencyNames;
        this.groupedFx = groupedFx;
    }

    @Override
    /**
     * Provides csv-like notation for String representation.
     */
    public String toString() {

        //todo move this into view... or not ?
        StringBuffer buff = new StringBuffer();
        buff.append("Date");
        for (String currency: currencyNames) {
            buff.append(",").append(currency);
        }
        buff.append("\n");
        SimpleDateFormat sdf =new SimpleDateFormat("dd-MM-yyyy");
        for (Map.Entry<Date, Map<String, Float>> line : groupedFx.entrySet()) {
            boolean first=true;
            boolean needNewLine = false;
            for (String currency: currencyNames) {
                Float value = line.getValue().get(currency);
                if (value == null || value.isNaN()) {
                    continue;
                }
                if (first) {
                    buff.append(sdf.format(line.getKey()));
                    first = false;
                    needNewLine = true;
                }
                buff.append(",").append(value);
            }
            if (needNewLine) {
                buff.append("\n");
            }
        }

        return buff.toString();
    }
}
