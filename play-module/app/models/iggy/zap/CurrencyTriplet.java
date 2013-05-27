package models.iggy.zap;

import java.util.Date;

/**
 * VO class to hold date, currency name and exchange rate
 */
public class CurrencyTriplet {
    public final Date date;
    public final String currency;
    public final float rate;

    public CurrencyTriplet(Date date, String currency, float rate) {
        this.date = date;
        this.currency = currency;
        this.rate = rate;
    }

    public static String mapKey(Date date, String currencyName) {
        return String.format("%1$tD_%2$s", date, currencyName);
    }
}
