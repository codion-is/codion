/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.textfield;

import is.codion.common.state.State;
import is.codion.swing.common.ui.Utilities;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.Temporal;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TemporalInputPanelTest {

  @Test
  void setText() {
    TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class, "dd.MM.yyyy").build();
    TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
    panel.getInputField().setText("01.03.2010");
    assertEquals(LocalDate.parse("01.03.2010", DateTimeFormatter.ofPattern("dd.MM.yyyy")), panel.getTemporal());
  }

  @Test
  void setDate() {
    DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class, "dd.MM.yyyy").build();
    TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
    panel.setTemporal(LocalDate.parse("03.04.2010", formatter));
    assertEquals("03.04.2010", panel.getInputField().getText());
    panel.setTemporal(null);
    assertEquals("__.__.____", panel.getInputField().getText());
  }

  @Test
  void getDate() {
    TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class, "dd.MM.yyyy").build();
    TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
    assertFalse(panel.getOptional().isPresent());
    panel.getInputField().setText("03");
    assertFalse(panel.getOptional().isPresent());
    panel.getInputField().setText("03.04");
    assertFalse(panel.getOptional().isPresent());
    panel.getInputField().setText("03.04.2010");
    assertNotNull(panel.getTemporal());
  }

  @Test
  void unsupportedType() {
    TemporalField<LocalTime> field = TemporalField.builder(LocalTime.class, "hh:MM").build();
    TemporalInputPanel<LocalTime> panel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
    assertFalse(panel.getCalendarButton().isPresent());
  }

  @Test
  void constructorNullInputField() {
    assertThrows(NullPointerException.class, () -> new TemporalInputPanel<>(null, new DefaultCalendarProvider()));
  }

  @Test
  void enabledState() throws InterruptedException {
    State enabledState = State.state();
    TemporalField<LocalDate> field = TemporalField.builder(LocalDate.class, "dd.MM.yyyy").build();
    TemporalInputPanel<LocalDate> inputPanel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
    Utilities.linkToEnabledState(enabledState, inputPanel);
    assertFalse(field.isEnabled());
    Optional<JButton> calendarButton = inputPanel.getCalendarButton();
    assertTrue(calendarButton.isPresent());
    assertFalse(calendarButton.get().isEnabled());
    enabledState.set(true);
    Thread.sleep(100);
    assertTrue(calendarButton.get().isEnabled());
  }

  private static final class DefaultCalendarProvider implements TemporalInputPanel.CalendarProvider {

    @Override
    public <T extends Temporal> Optional<T> getTemporal(Class<T> temporalClass, JComponent dialogOwner,
                                                        T initialValue) {
      return Optional.empty();
    }

    @Override
    public <T extends Temporal> boolean supports(Class<T> temporalClass) {
      return LocalDate.class.equals(temporalClass) || LocalDateTime.class.equals(temporalClass);
    }
  }
}
