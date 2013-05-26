package models.iggy.zap;

import models.iggy.zap.fx.CubeType;
import models.iggy.zap.fx.CurrencyCube;
import models.iggy.zap.fx.EnvelopeType;
import models.iggy.zap.fx.TimeCube;
import org.apache.commons.io.IOUtils;
import play.jobs.Job;
import play.jobs.OnApplicationStart;
import play.libs.F;
import play.libs.WS;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.Date;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Job to pre-populate currency triplets.
 */
@OnApplicationStart
public class CurrencyTripletPopulationJob extends Job {

    private static ConcurrentMap<String, F.Promise<CurrencyTriplet>> promisedTriplets = new ConcurrentHashMap<String, F.Promise<CurrencyTriplet>>();
    private static ConcurrentMap<Date, Date> availableDates = new ConcurrentHashMap<Date, Date>();
    private static ConcurrentMap<String, String> availableCurrencies = new ConcurrentHashMap<String, String>();

    public static ConcurrentMap<String, F.Promise<CurrencyTriplet>> getPromisedTriplets() {
        return promisedTriplets;
    }

    public static ConcurrentMap<Date, Date> getAvailableDates() {
        return availableDates;
    }

    public static ConcurrentMap<String, String> getAvailableCurrencies() {
        return availableCurrencies;
    }

    public static void addCurrencyTriplet(CurrencyTriplet currencyTriplet) {
        String key = CurrencyTriplet.mapKey(currencyTriplet.date, currencyTriplet.currency);
        promisedTriplets.putIfAbsent(key,
                            new F.Promise<CurrencyTriplet>());
        //populate
        promisedTriplets.get(key).invoke(currencyTriplet);
        availableDates.put(currencyTriplet.date, currencyTriplet.date);
        availableCurrencies.put(currencyTriplet.currency, currencyTriplet.currency);
    }

    @Override
    public void doJob() throws Exception {
        WS.HttpResponse response = WS.url("http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist.xml").get();
        InputStream is = response.getStream();
        try {
            EnvelopeType envelope = JAXBContext.newInstance(EnvelopeType.class).createUnmarshaller().unmarshal(new StreamSource(is), EnvelopeType.class).getValue();
            CubeType ct = envelope.getCube();
            for (TimeCube tc : ct.getCube()) {
                Date date = tc.getTime().toGregorianCalendar().getTime();
                for (CurrencyCube cc : tc.getCube()) {
                    addCurrencyTriplet(new CurrencyTriplet(date, cc.getCurrency(), cc.getRate() == null ? Float.NaN : cc.getRate()));
                }
            }
        } finally {
            IOUtils.closeQuietly(is);
        }

    }
}
