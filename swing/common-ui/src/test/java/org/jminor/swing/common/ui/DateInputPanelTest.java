/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.State;
import org.jminor.common.States;

import org.junit.Test;

import javax.swing.JFormattedTextField;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

public class DateInputPanelTest {

  @Test
  public void constructor() throws Exception {
    final DateInputPanel panel = new DateInputPanel(new Date(), new SimpleDateFormat("dd.MM.yyyy"));
    assertEquals("dd.MM.yyyy", panel.getFormatPattern());
    assertNotNull(panel.getInputField());
    assertNotNull(panel.getButton());
  }

  @Test
  public void setText() throws ParseException {
    final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
    final DateInputPanel panel = new DateInputPanel(null, format);
    panel.getInputField().setText("01.03.2010");
    assertEquals(format.parse("01.03.2010"), panel.getDate());
  }

  @Test
  public void setDate() throws ParseException {
    final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
    final DateInputPanel panel = new DateInputPanel(null, format);
    panel.setDate(format.parse("03.04.2010"));
    assertEquals("03.04.2010", panel.getInputField().getText());
    panel.setDate(null);
    assertEquals("__.__.____", panel.getInputField().getText());
  }

  @Test
  public void getDate() throws ParseException {
    final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
    final DateInputPanel panel = new DateInputPanel(null, format);
    assertNull(panel.getDate());
    panel.getInputField().setText("03");
    assertNull(panel.getDate());
    panel.getInputField().setText("03.04");
    assertNull(panel.getDate());
    panel.getInputField().setText("03.04.2010");
    assertNotNull(panel.getDate());
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullInputField() {
    new DateInputPanel(null, new SimpleDateFormat("dd.MM.yyyy"), true, null);
  }

  @Test(expected = NullPointerException.class)
  public void constructorNullDateFormat() {
    new DateInputPanel(new JFormattedTextField(), null, true, null);
  }

  @Test
  public void enabledState() {
    final State enabledState = States.state();
    final JFormattedTextField txtField = new JFormattedTextField();
    final DateInputPanel inputPanel = new DateInputPanel(txtField, new SimpleDateFormat("dd.MM.yyyy"), true, enabledState.getObserver());
    assertFalse(txtField.isEnabled());
    assertFalse(inputPanel.getButton().isEnabled());
    enabledState.setActive(true);
    assertTrue(txtField.isEnabled());
    assertTrue(inputPanel.getButton().isEnabled());
  }

  @Test
  public void intervalInputPanel() throws ParseException {
    final SimpleDateFormat format = new SimpleDateFormat("dd.MM.yyyy");
    final DateInputPanel.IntervalInputPanel inputPanel = new DateInputPanel.IntervalInputPanel(format,
            new DateInputPanel.DateInterval(format.parse("01.03.2010"), format.parse("31.03.2010")));
    assertEquals("01.03.2010", inputPanel.getFromInputPanel().getInputField().getText());
    assertEquals("31.03.2010", inputPanel.getToInputPanel().getInputField().getText());
    DateInputPanel.DateInterval interval = inputPanel.getDateInterval();
    assertNotNull(interval);
    inputPanel.getFromInputPanel().getInputField().setText("");
    interval = inputPanel.getDateInterval();
    assertNull(interval);
  }
}
