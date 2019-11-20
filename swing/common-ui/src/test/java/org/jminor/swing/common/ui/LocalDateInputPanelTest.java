/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.state.State;
import org.jminor.common.state.States;

import org.junit.jupiter.api.Test;

import javax.swing.JFormattedTextField;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class LocalDateInputPanelTest {

  @Test
  public void constructor() {
    final LocalDateInputPanel panel = new LocalDateInputPanel(LocalDate.now(), "dd.MM.yyyy");
    assertEquals("dd.MM.yyyy", panel.getDateFormat());
    assertNotNull(panel.getInputField());
    assertNotNull(panel.getCalendarButton());
  }

  @Test
  public void setText() {
    final TemporalInputPanel panel = new LocalDateInputPanel(null, "dd.MM.yyyy");
    panel.getInputField().setText("01.03.2010");
    assertEquals(LocalDate.parse("01.03.2010", DateTimeFormatter.ofPattern("dd.MM.yyyy")), panel.getTemporal());
  }

  @Test
  public void setDate() {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    final LocalDateInputPanel panel = new LocalDateInputPanel(null, "dd.MM.yyyy");
    panel.setTemporal(LocalDate.parse("03.04.2010", formatter));
    assertEquals("03.04.2010", panel.getInputField().getText());
    panel.setTemporal(null);
    assertEquals("__.__.____", panel.getInputField().getText());
  }

  @Test
  public void getDate() {
    final LocalDateInputPanel panel = new LocalDateInputPanel(null, "dd.MM.yyyy");
    assertNull(panel.getTemporal());
    panel.getInputField().setText("03");
    assertNull(panel.getTemporal());
    panel.getInputField().setText("03.04");
    assertNull(panel.getTemporal());
    panel.getInputField().setText("03.04.2010");
    assertNotNull(panel.getTemporal());
  }

  @Test
  public void constructorNullInputField() {
    assertThrows(NullPointerException.class, () -> new LocalDateInputPanel(null, "dd.MM.yyyy", true, null));
  }

  @Test
  public void constructorNullDateFormat() {
    assertThrows(NullPointerException.class, () -> new LocalDateInputPanel(new JFormattedTextField(), null, true, null));
  }

  @Test
  public void enabledState() {
    final State enabledState = States.state();
    final JFormattedTextField textField = new JFormattedTextField();
    final LocalDateInputPanel inputPanel = new LocalDateInputPanel(textField, "dd.MM.yyyy", true, enabledState.getObserver());
    assertFalse(textField.isEnabled());
    assertFalse(inputPanel.getCalendarButton().isEnabled());
    enabledState.set(true);
    assertTrue(textField.isEnabled());
    assertTrue(inputPanel.getCalendarButton().isEnabled());
  }
}
