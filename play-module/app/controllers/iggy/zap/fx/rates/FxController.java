package controllers.iggy.zap.fx.rates;

import models.iggy.zap.CsvTabulator;
import models.iggy.zap.fx.CubeType;
import models.iggy.zap.fx.CurrencyCube;
import models.iggy.zap.fx.EnvelopeType;
import models.iggy.zap.fx.TimeCube;
import play.jobs.Job;
import play.libs.F;
import play.libs.WS;
import play.mvc.Before;
import play.mvc.Controller;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.*;
import java.util.concurrent.ExecutionException;

public class FxController extends Controller{

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

        F.Promise<F.T3<WS.HttpResponse, CubeType, CsvTabulator>> bwah =  F.Promise.wait3(currencyFeed, currencies, transformedCurrencies);
        await(bwah);

        renderText(bwah.get()._3);
    }
}
