package org.jminor.common.ui.input;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.formats.DateFormats;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateInputProviderTest {

  @Test
  public void test() {
    final Date date = DateUtil.floorDate(new Date());
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_DASH);
    DateInputProvider provider = new DateInputProvider(date, format);
    assertEquals(date, provider.getValue());

    provider = new DateInputProvider(null, format);
    assertNull(provider.getValue());

    provider.getInputComponent().getInputField().setText(format.format(date));
    assertEquals(date, provider.getValue());
  }
}
