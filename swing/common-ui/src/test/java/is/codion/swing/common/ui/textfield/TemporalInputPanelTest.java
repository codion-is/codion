/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.textfield;

import is.codion.common.state.State;
import is.codion.swing.common.ui.Utilities;

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

public class TemporalInputPanelTest {

  @Test
  void constructor() {
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    field.setTemporal(LocalDate.now());
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
    assertEquals("dd.MM.yyyy", panel.getDateTimePattern());
    assertNotNull(panel.getInputField());
  }

  @Test
  void setText() {
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
    panel.getInputField().setText("01.03.2010");
    assertEquals(LocalDate.parse("01.03.2010", DateTimeFormatter.ofPattern("dd.MM.yyyy")), panel.getTemporal());
  }

  @Test
  void setDate() {
    final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
    panel.setTemporal(LocalDate.parse("03.04.2010", formatter));
    assertEquals("03.04.2010", panel.getInputField().getText());
    panel.setTemporal(null);
    assertEquals("__.__.____", panel.getInputField().getText());
  }

  @Test
  void getDate() {
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    final TemporalInputPanel<LocalDate> panel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
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
    assertThrows(NullPointerException.class, () -> new TemporalInputPanel<>(null, new DefaultCalendarProvider()));
  }

  @Test
  void enabledState() throws InterruptedException {
    final State enabledState = State.state();
    final TemporalField<LocalDate> field = TemporalField.localDateField("dd.MM.yyyy");
    final TemporalInputPanel<LocalDate> inputPanel = new TemporalInputPanel<>(field, new DefaultCalendarProvider());
    Utilities.linkToEnabledState(enabledState, inputPanel);
    assertFalse(field.isEnabled());
    assertFalse(inputPanel.getCalendarButton().isEnabled());
    enabledState.set(true);
    Thread.sleep(100);
    assertTrue(field.isEnabled());
    assertTrue(inputPanel.getCalendarButton().isEnabled());
  }

  private static final class DefaultCalendarProvider implements TemporalInputPanel.CalendarProvider {

    @Override
    public Optional<LocalDate> getLocalDate(final String dialogTitle, final JComponent dialogOwner,
                                            final LocalDate startDate) {
      return Optional.empty();
    }

    @Override
    public Optional<LocalDateTime> getLocalDateTime(final String dialogTitle, final JComponent dialogOwner,
                                                    final LocalDateTime startDateTime) {
      return Optional.empty();
    }
  }
}
