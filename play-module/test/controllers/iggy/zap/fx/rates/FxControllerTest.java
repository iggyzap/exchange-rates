package controllers.iggy.zap.fx.rates;

import org.joda.time.MutableDateTime;
import org.joda.time.ReadWritableDateTime;
import org.junit.Test;

import java.util.Date;

import static junit.framework.Assert.assertEquals;
import static junit.framework.Assert.assertNotNull;
import static org.junit.Assert.assertSame;

public class FxControllerTest {

    @Test
    public void testAdd() throws Exception {
        assertNotNull(FxController.add(new Date(), 0));
    }

    @Test
    public void testAddIgnores0Days() throws Exception {
        Date date = new Date();
        assertSame(date, FxController.add(date, 0));
    }

    @Test
    public void testAddWorks() throws Exception {
        ReadWritableDateTime foo = new MutableDateTime();
        Date date = foo.toDateTime().toDate();
        foo.addDays(1);

        assertEquals(foo.toDateTime().toDate(), FxController.add(date, 1));
    }

}
