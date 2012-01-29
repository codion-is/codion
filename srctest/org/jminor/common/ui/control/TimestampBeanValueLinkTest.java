/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import org.junit.Test;

import javax.swing.JFormattedTextField;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;

public class TimestampBeanValueLinkTest {

  private Timestamp timestamp;
  private Event evtDateValueChanged = Events.event();

  @Test
  public void test() throws Exception {
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_DOT);

    final JFormattedTextField txtString = UiUtil.createFormattedField(DateUtil.getDateMask(format));
    new TimestampBeanValueLink(txtString, this, "dateValue", evtDateValueChanged, LinkType.READ_WRITE, format);
    assertEquals("String value should be empty on initialization", "__.__.____", txtString.getText());

    final Timestamp date = new Timestamp(format.parse("03.10.1975").getTime());

    setDateValue(date);
    assertEquals("String value should be '03.10.1975'", "03.10.1975", txtString.getText());
    txtString.setText("03.03.1983");
    assertEquals("String value should be 03.03.1983", new Timestamp(format.parse("03.03.1983").getTime()), timestamp);
    txtString.setText("");
    assertNull("String value should be empty", timestamp);
  }

  public Timestamp getDateValue() {
    return timestamp;
  }

  public void setDateValue(final Timestamp timestamp) {
    this.timestamp = timestamp;
    evtDateValueChanged.fire();
  }
}