/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.time;

import is.codion.common.state.State;
import is.codion.swing.common.ui.textfield.TemporalField;

import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

import static org.junit.jupiter.api.Assertions.*;

public class TemporalInputPanelTest {

  @Test
  public void constructor() {
    final TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class).dateTimePattern("dd.MM.yyyy").build();
    field.setTemporal(LocalDate.now());
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    assertEquals("dd.MM.yyyy", panel.getDateTimePattern());
    assertNotNull(panel.getInputField());
  }

  @Test
  public void setText() {
    final TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class).dateTimePattern("dd.MM.yyyy").build();
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    panel.getInputField().setText("01.03.2010");
    assertEquals(LocalDate.parse("01.03.2010", DateTimeFormatter.ofPattern("dd.MM.yyyy")), panel.getTemporal());
  }

  @Test
  public void setDate() {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    final TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class).dateTimePattern("dd.MM.yyyy").build();
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
    panel.setTemporal(LocalDate.parse("03.04.2010", formatter));
    assertEquals("03.04.2010", panel.getInputField().getText());
    panel.setTemporal(null);
    assertEquals("__.__.____", panel.getInputField().getText());
  }

  @Test
  public void getDate() {
    final TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class).dateTimePattern("dd.MM.yyyy").build();
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field);
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
    assertThrows(NullPointerException.class, () -> new TemporalInputPanel<>(null));
  }

  @Test
  public void enabledState() {
    final State enabledState = State.state();
    final TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class).dateTimePattern("dd.MM.yyyy").build();
    final TemporalInputPanel<LocalDate> inputPanel = new TemporalInputPanel<>(field, enabledState.getObserver());
    assertFalse(field.isEnabled());
    assertFalse(inputPanel.getCalendarButton().isEnabled());
    SwingUtilities.invokeLater(() -> {
      enabledState.set(true);
      assertTrue(field.isEnabled());
      assertTrue(inputPanel.getCalendarButton().isEnabled());
    });
  }
}
