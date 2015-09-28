/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.swing.ui;

import org.jminor.common.model.State;
import org.jminor.common.model.States;

import org.junit.Test;

import javax.swing.JFormattedTextField;
import java.text.SimpleDateFormat;
import java.util.Date;

import static org.junit.Assert.*;

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

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullInputField() {
    new DateInputPanel(null, new SimpleDateFormat("dd.MM.yyyy"), true, null);
  }

  @Test(expected = IllegalArgumentException.class)
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
}
