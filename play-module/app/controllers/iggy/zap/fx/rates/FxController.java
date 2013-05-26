package controllers.iggy.zap.fx.rates;

import models.iggy.zap.CsvTabulator;
import models.iggy.zap.fx.CubeType;
import models.iggy.zap.fx.CurrencyCube;
import models.iggy.zap.fx.EnvelopeType;
import models.iggy.zap.fx.TimeCube;
import play.jobs.Job;
import play.libs.F;
import play.libs.WS;
import play.mvc.Controller;

import javax.xml.bind.JAXBContext;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class FxController extends Controller {

    /**
     * Filtering currencies is 4-stpe process - 1) obtain 90 day rolling data from ECB
     * 2) Parse it from XML into POJO
     * 3) Group into data that can be directly rendered as CSV format
     * 4) Dump CSV format
     * @throws ExecutionException
     * @throws InterruptedException
     */
    public static void filterCurrencies() throws ExecutionException, InterruptedException {
        final F.Promise<WS.HttpResponse> currencyFeed = WS.url("http://www.ecb.europa.eu/stats/eurofxref/eurofxref-hist-90d.xml").getAsync();

        final F.Promise<CubeType> currencies = new Job<CubeType>() {
            @Override
            public CubeType doJobWithResult() throws Exception {
                InputStream is = currencyFeed.get().getStream();
                EnvelopeType envelope = JAXBContext.newInstance(EnvelopeType.class).createUnmarshaller().unmarshal(new StreamSource(is), EnvelopeType.class).getValue();
                return envelope.getCube();
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
