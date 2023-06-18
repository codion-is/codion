/*
 * Copyright (c) 2019 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.state.State;
import is.codion.swing.common.ui.Utilities;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TemporalInputPanelTest {

  @Test
  void setText() {
    TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class, "dd.MM.yyyy").build();
    TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    panel.inputField().setText("01.03.2010");
    assertEquals(LocalDate.parse("01.03.2010", DateTimeFormatter.ofPattern("dd.MM.yyyy")), panel.getTemporal());
  }

  @Test
  void setTemporal() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class, "dd.MM.yyyy").build();
    TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    panel.setTemporal(LocalDate.parse("03.04.2010", formatter));
    assertEquals("03.04.2010", panel.inputField().getText());
    panel.setTemporal(null);
    assertEquals("__.__.____", panel.inputField().getText());
  }

  @Test
  void getTemporal() {
    TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class, "dd.MM.yyyy").build();
    TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    assertFalse(panel.optional().isPresent());
    panel.inputField().setText("03");
    assertFalse(panel.optional().isPresent());
    panel.inputField().setText("03.04");
    assertFalse(panel.optional().isPresent());
    panel.inputField().setText("03.04.2010");
    assertNotNull(panel.getTemporal());
  }

  @Test
  void unsupportedType() {
    TemporalField<LocalTime> field = TemporalField.builder(LocalTime.class, "hh:MM").build();
    TemporalInputPanel<LocalTime> panel = new TemporalInputPanel<>(field);
    assertFalse(panel.calendarButton().isPresent());
  }

  @Test
  void constructorNullInputField() {
    assertThrows(NullPointerException.class, () -> new TemporalInputPanel<>(null));
  }

  @Test
  void enabledState() throws InterruptedException {
    State enabledState = State.state();
    TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class, "dd.MM.yyyy").build();
    TemporalInputPanel<LocalDate> inputPanel = new TemporalInputPanel<>(field);
    Utilities.linkToEnabledState(enabledState, inputPanel);
    assertFalse(field.isEnabled());
    Optional<JButton> calendarButton = inputPanel.calendarButton();
    assertTrue(calendarButton.isPresent());
    assertFalse(calendarButton.get().isEnabled());
    enabledState.set(true);
    Thread.sleep(100);
    assertTrue(calendarButton.get().isEnabled());
  }
}
