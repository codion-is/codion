package org.jminor.common.ui;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import org.junit.Test;

import java.text.SimpleDateFormat;
import java.util.Date;

public class DateInputPanelTest {

  @Test
  public void test() throws Exception {
    final Date now = new Date();
    final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
    final DateInputPanel panel = new DateInputPanel(now, format);
    assertEquals("dd.MM.yyyy", panel.getFormatPattern());
    assertNotNull(panel.getInputField());
    assertNotNull(panel.getButton());

    panel.getInputField().setText("01.03.2010");

    assertEquals(format.parse("01.03.2010"), panel.getDate());
  }
}
