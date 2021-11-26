/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.state.State;
import is.codion.swing.common.ui.Utilities;

import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class TemporalInputPanelTest {

  @Test
  void constructor() {
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    field.setTemporal(LocalDate.now());
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    assertEquals("dd.MM.yyyy", panel.getDateTimePattern());
    assertNotNull(panel.getInputField());
  }

  @Test
  void setText() {
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    panel.getInputField().setText("01.03.2010");
    assertEquals(LocalDate.parse("01.03.2010", DateTimeFormatter.ofPattern("dd.MM.yyyy")), panel.getTemporal());
  }

  @Test
  void setDate() {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    panel.setTemporal(LocalDate.parse("03.04.2010", formatter));
    assertEquals("03.04.2010", panel.getInputField().getText());
    panel.setTemporal(null);
    assertEquals("__.__.____", panel.getInputField().getText());
  }

  @Test
  void getDate() {
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    assertFalse(panel.getOptional().isPresent());
    panel.getInputField().setText("03");
    assertFalse(panel.getOptional().isPresent());
    panel.getInputField().setText("03.04");
    assertFalse(panel.getOptional().isPresent());
    panel.getInputField().setText("03.04.2010");
    assertNotNull(panel.getTemporal());
  }

  @Test
  void constructorNullInputField() {
    assertThrows(NullPointerException.class, () -> new TemporalInputPanel<>(null));
  }

  @Test
  void enabledState() throws InterruptedException {
    final State enabledState = State.state();
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    final TemporalInputPanel<LocalDate> inputPanel = new TemporalInputPanel<>(field);
    Utilities.linkToEnabledState(enabledState, inputPanel);
    assertFalse(field.isEnabled());
    assertFalse(inputPanel.getCalendarButton().isEnabled());
    enabledState.set(true);
    Thread.sleep(100);
    assertTrue(field.isEnabled());
    assertTrue(inputPanel.getCalendarButton().isEnabled());
  }
}
