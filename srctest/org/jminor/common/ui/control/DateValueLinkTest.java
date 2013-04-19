/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.DateUtil;
import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.formats.DateFormats;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.ValueLinks;

import org.junit.Test;

import javax.swing.JFormattedTextField;
import java.sql.Time;
import java.sql.Timestamp;
import java.sql.Types;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class DateValueLinkTest {

  private Time timeValue;
  private Date dateValue;
  private Timestamp timestamp;
  private final Event evtTimeValueChanged = Events.event();
  private final Event evtDateValueChanged = Events.event();
  private final Event evtTimestampValueChanged = Events.event();

  @Test
  public void testTime() throws Exception {
    final SimpleDateFormat format = DateFormats.getDateFormat("HH:mm");

    final JFormattedTextField txtString = UiUtil.createFormattedField(DateUtil.getDateMask(format));
    ValueLinks.dateValueLink(txtString, this, "time", evtTimeValueChanged, false, format, Types.TIME);
    assertEquals("String value should be empty on initialization", "__:__", txtString.getText());

    final Time date = new Time(format.parse("22:42").getTime());

    setTime(date);
    assertEquals("String value should be '22:42'", "22:42", txtString.getText());
    txtString.setText("23:50");
    assertEquals("String value should be 23:50", format.parse("23:50"), timeValue);
    txtString.setText("");
    assertNull("String value should be empty", timeValue);
  }

  @Test
  public void testDate() throws Exception {
    final SimpleDateFormat format = DateFormats.getDateFormat(DateFormats.SHORT_DOT);

    final JFormattedTextField txtString = UiUtil.createFormattedField(DateUtil.getDateMask(format));
    ValueLinks.dateValueLink(txtString, this, "date", evtDateValueChanged, false, format, Types.DATE);
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
    ValueLinks.dateValueLink(txtString, this, "timestamp", evtTimestampValueChanged, false, format, Types.TIMESTAMP);
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

  public Time getTime() {
    return timeValue;
  }

  public void setTime(final Time timeValue) {
    this.timeValue = timeValue;
    evtTimeValueChanged.fire();
  }
}
