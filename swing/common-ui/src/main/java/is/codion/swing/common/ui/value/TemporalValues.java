/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.swing.common.ui.time.TemporalInputPanel;

import javax.swing.JFormattedTextField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.OffsetDateTime;
import java.time.temporal.Temporal;

/**
 * Utility class for temporal {@link ComponentValue} instances.
 */
public final class TemporalValues {

  private TemporalValues() {}

  /**
   * Instantiates a new {@link ComponentValue} for {@link Temporal} values.
   * @param inputPanel the input panel to use
   * @param <V> the temporal value type
   * @return a Value bound to the given component
   */
  public static <V extends Temporal> ComponentValue<V, TemporalInputPanel<V>> temporalValue(final TemporalInputPanel<V> inputPanel) {
    return new TemporalInputPanelValue<>(inputPanel);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalTime, JFormattedTextField> localTimeValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat) {
    return localTimeValue(textComponent, dateFormat, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalTime, JFormattedTextField> localTimeValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat,
                                                                              final UpdateOn updateOn) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOn, LocalTime::parse);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDate, JFormattedTextField> localDateValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat) {
    return localDateValue(textComponent, dateFormat, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDate, JFormattedTextField> localDateValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat,
                                                                              final UpdateOn updateOn) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOn, LocalDate::parse);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDateTime, JFormattedTextField> localDateTimeValue(final JFormattedTextField textComponent,
                                                                                      final String dateFormat) {
    return localDateTimeValue(textComponent, dateFormat, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDateTime, JFormattedTextField> localDateTimeValue(final JFormattedTextField textComponent,
                                                                                      final String dateFormat,
                                                                                      final UpdateOn updateOn) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOn, LocalDateTime::parse);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static ComponentValue<OffsetDateTime, JFormattedTextField> offsetDateTimeValue(final JFormattedTextField textComponent,
                                                                                        final String dateFormat) {
    return offsetDateTimeValue(textComponent, dateFormat, UpdateOn.KEYSTROKE);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOn specifies when the underlying value should be updated
   * @return a Value bound to the given component
   */
  public static ComponentValue<OffsetDateTime, JFormattedTextField> offsetDateTimeValue(final JFormattedTextField textComponent,
                                                                                        final String dateFormat,
                                                                                        final UpdateOn updateOn) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOn, OffsetDateTime::parse);
  }
}
