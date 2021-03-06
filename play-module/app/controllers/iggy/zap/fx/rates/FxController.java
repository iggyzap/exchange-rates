package controllers.iggy.zap.fx.rates;

import models.iggy.zap.CsvTabulator;
import models.iggy.zap.CurrencyTriplet;
import models.iggy.zap.fx.CubeType;
import models.iggy.zap.fx.CurrencyCube;
import models.iggy.zap.fx.EnvelopeType;
import models.iggy.zap.fx.TimeCube;
import org.apache.commons.io.IOUtils;
import org.joda.time.MutableDateTime;
import org.joda.time.ReadWritableDateTime;
import play.Logger;
import play.data.binding.As;
import play.jobs.Job;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

import static models.iggy.zap.CurrencyTripletPopulationJob.getAvailableCurrencies;
import static models.iggy.zap.CurrencyTripletPopulationJob.getAvailableDates;
import static models.iggy.zap.CurrencyTripletPopulationJob.getPromisedTriplets;

public class FxController extends Controller {

    public static void listDates () {
        List<Date> allDates = new ArrayList<Date>(getAvailableDates().keySet());
        Collections.sort (allDates);
        renderText(allDates);
    }

    public static void listCurrencies () {
        renderText(getAvailableCurrencies().keySet());
    }

    /**
     * Supports pseudo-real-time querying for a X days for currency - NB - this will block indefinitely if days will
     * never be fetched. If Play promise holds - final await will allow other requests to succeed!
     *
     * @param from
     * @param days
     * @param currency
     */
    public static void currencyHistory(@As("dd-MM-yyyy") Date from, int days, String currency) {
        final Set<String> names = Collections.singleton(currency);

        if (from.after(new Date())) {
            from = new Date();
        }

        if (days < 0) {
            from = add(from, days);
            days *= -1;
        }
//        Logger.info("Date : %1$s", from);
        final List<F.Promise<CurrencyTriplet>> toWait = new ArrayList<F.Promise<CurrencyTriplet>>(days);
        for (int i = 0; i < days; i++) {
            //this will use ide that we either already have those triplets, or they will be populated in a future
            String key = CurrencyTriplet.mapKey(add(from, i), currency);
//            Logger.info("Adding key %1$s", key);
            toWait.add(getPromisedTriplets().putIfAbsent(key, new F.Promise<CurrencyTriplet>()));
        }

        F.Promise<CsvTabulator> promise = new Job<CsvTabulator>() {
            @Override
            public CsvTabulator doJobWithResult() throws Exception {
                SortedMap<Date, Map<String, Float>> groupedFx = new TreeMap<Date, Map<String, Float>>();
//                Logger.info("Expected values: %1$s", toWait.size());
                for (F.Promise<CurrencyTriplet> promisedTriplet : toWait) {
                    addToMap(groupedFx, promisedTriplet.get());
                }

                return new CsvTabulator(names, groupedFx);
            }
        }.now();

        renderText(await(promise));
    }

    private static void addToMap(SortedMap<Date, Map<String, Float>> groupedFx, CurrencyTriplet currencyTriplet) {
        Map<String, Float> map = groupedFx.get(currencyTriplet.date);
//        Logger.info("Triplet %1$s %2$s %3$s", currencyTriplet.date, currencyTriplet.currency, currencyTriplet.rate);
        if (map == null) {
            map = new HashMap<String, Float>();
            groupedFx.put(currencyTriplet.date, map);
        }

        map.put(currencyTriplet.currency, currencyTriplet.rate);
    }

    protected static Date add(Date from, int days) {
        Date result = from;
        if (days != 0) {
            ReadWritableDateTime t = new MutableDateTime(from);
            t.addDays(days);
            result = new Date(t.getMillis());
        }
        return result;
    }

    /**
     * Filtering currencies is 4-stpe process - 1) obtain 90 day rolling data from ECB
     * 2) Parse it from XML into POJO
     * 3) Group into data that can be directly rendered as CSV format
     * 4) Dump CSV format
     *
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void filterCurrencies() throws ExecutionException, InterruptedException {
        final F.Promise<WS.HttpResponse> currencyFeed = WS.url("http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist-90d.xml").getAsync();

        final F.Promise<CubeType> currencies = new Job<CubeType>() {
            @Override
            public CubeType doJobWithResult() throws Exception {
                InputStream is = currencyFeed.get().getStream();
                try {
                    EnvelopeType envelope = JAXBContext.newInstance(EnvelopeType.class).createUnmarshaller().unmarshal(new StreamSource(is), EnvelopeType.class).getValue();
                    return envelope.getCube();
                } finally {
                    IOUtils.closeQuietly(is);
                }
            }
        }.now();

        final F.Promise<CsvTabulator> transformedCurrencies = new Job<CsvTabulator>() {
            @Override
            public CsvTabulator doJobWithResult() throws Exception {

                CubeType ct = currencies.get();
                //we have to collect what are possible currencies in whole data set, also this will allow to preserve
                // currency order when writing results
                Set<String> currencyNames = new HashSet<String>();
                SortedMap<Date, Map<String, Float>> groupedFx = new TreeMap<Date, Map<String, Float>>();
                for (TimeCube tc : ct.getCube()) {
                    Date date = tc.getTime().toGregorianCalendar().getTime();
                    Map<String, Float> currencies = new HashMap<String, Float>();
                    //todo assert!
                    groupedFx.put(date, currencies);
                    for (CurrencyCube cc : tc.getCube()) {
                        currencyNames.add(cc.getCurrency());
                        currencies.put(cc.getCurrency(), cc.getRate());
                    }
                }

                return new CsvTabulator(currencyNames, groupedFx);
            }
        }.now();

        F.Promise<F.T3<WS.HttpResponse, CubeType, CsvTabulator>> bwah = F.Promise.wait3(currencyFeed, currencies, transformedCurrencies);
        renderText(await(bwah)._3);
    }
}
