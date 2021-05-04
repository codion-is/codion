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

public class LocalDateInputPanelTest {

  @Test
  public void constructor() {
    final TemporalField<LocalDate> field = new TemporalField<>(LocalDate.class, "dd.MM.yyyy");
    field.setTemporal(LocalDate.now());
    final LocalDateInputPanel panel = new LocalDateInputPanel(field, true, null);
    assertEquals("dd.MM.yyyy", panel.getDateTimePattern());
    assertNotNull(panel.getInputField());
    assertNotNull(panel.getCalendarButton());
  }

  @Test
  public void setText() {
    final TemporalField<LocalDate> field = new TemporalField<>(LocalDate.class, "dd.MM.yyyy");
    final LocalDateInputPanel panel = new LocalDateInputPanel(field, false, null);
    panel.getInputField().setText("01.03.2010");
    assertEquals(LocalDate.parse("01.03.2010", DateTimeFormatter.ofPattern("dd.MM.yyyy")), panel.getTemporal());
  }

  @Test
  public void setDate() {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    final TemporalField<LocalDate> field = new TemporalField<>(LocalDate.class, "dd.MM.yyyy");
    final LocalDateInputPanel panel = new LocalDateInputPanel(field, false, null);
    panel.setTemporal(LocalDate.parse("03.04.2010", formatter));
    assertEquals("03.04.2010", panel.getInputField().getText());
    panel.setTemporal(null);
    assertEquals("__.__.____", panel.getInputField().getText());
  }

  @Test
  public void getDate() {
    final TemporalField<LocalDate> field = new TemporalField<>(LocalDate.class, "dd.MM.yyyy");
    final LocalDateInputPanel panel = new LocalDateInputPanel(field, false, null);
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
    assertThrows(NullPointerException.class, () -> new LocalDateInputPanel(null, true, null));
  }

  @Test
  public void enabledState() {
    final State enabledState = State.state();
    final TemporalField<LocalDate> field = new TemporalField<>(LocalDate.class, "dd.MM.yyyy");
    final LocalDateInputPanel inputPanel = new LocalDateInputPanel(field, true, enabledState.getObserver());
    assertFalse(field.isEnabled());
    assertFalse(inputPanel.getCalendarButton().isEnabled());
    SwingUtilities.invokeLater(() -> {
      enabledState.set(true);
      assertTrue(field.isEnabled());
      assertTrue(inputPanel.getCalendarButton().isEnabled());
    });
  }
}
