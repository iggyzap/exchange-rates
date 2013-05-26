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
            buff.append(sdf.format(line.getKey()));
            for (String currency: currencyNames) {
                Float value = line.getValue().get(currency);
                buff.append(",").append(value == null ? "" : value );
            }
            buff.append("\n");
        }

        return buff.toString();
    }
}
