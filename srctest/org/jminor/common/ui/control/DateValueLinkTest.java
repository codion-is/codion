/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.LinkType;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.ValueLinks;

import org.junit.Test;

import javax.swing.JFormattedTextField;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateValueLinkTest {

  private Date dateValue;
  private Timestamp timestamp;
  private final Event evtDateValueChanged = Events.event();
  private final Event evtTimestampValueChanged = Events.event();

  @Test
  public void testDate() throws Exception {
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_DOT);

    final JFormattedTextField txtString = UiUtil.createFormattedField(DateUtil.getDateMask(format));
    ValueLinks.dateValueLink(txtString, this, "date", evtDateValueChanged, LinkType.READ_WRITE, format, false);
    assertEquals("String value should be empty on initialization", "__.__.____", txtString.getText());

    final Date date = format.parse("03.10.1975");

    setDate(date);
    assertEquals("String value should be '03.10.1975'", "03.10.1975", txtString.getText());
    txtString.setText("03.03.1983");
    assertEquals("String value should be 03.03.1983", format.parse("03.03.1983"), dateValue);
    txtString.setText("");
    assertNull("String value should be empty", dateValue);
  }

  @Test
  public void testTimestamp() throws Exception {
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_TIMESTAMP);

    final JFormattedTextField txtString = UiUtil.createFormattedField(DateUtil.getDateMask(format));
    ValueLinks.dateValueLink(txtString, this, "timestamp", evtTimestampValueChanged, LinkType.READ_WRITE, format, true);
    assertEquals("String value should be empty on initialization", "__-__-__ __:__", txtString.getText());

    final Timestamp date = new Timestamp(format.parse("03-10-75 10:34").getTime());

    setTimestamp(date);
    assertEquals("String value should be '03-10-75 10:34'", "03-10-75 10:34", txtString.getText());
    txtString.setText("03-03-83 11:42");
    assertEquals("String value should be 03-03-83 11:42", new Timestamp(format.parse("03-03-83 11:42").getTime()), timestamp);
    txtString.setText("");
    assertNull("String value should be empty", timestamp);
  }

  public Timestamp getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(final Timestamp timestamp) {
    this.timestamp = timestamp;
    evtTimestampValueChanged.fire();
  }

  public Date getDate() {
    return dateValue;
  }

  public void setDate(final Date dateValue) {
    this.dateValue = dateValue;
    evtDateValueChanged.fire();
  }
}
