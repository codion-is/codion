/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.value.Value;
import org.jminor.swing.common.ui.time.TemporalInputPanel;

import javax.swing.JFormattedTextField;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
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
    return localTimeValue(textComponent, dateFormat, true);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalTime, JFormattedTextField> localTimeValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat,
                                                                              final boolean updateOnKeystroke) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOnKeystroke, LocalTime::parse);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param dateFormat the data format
   */
  public static void localTimeValueLink(final JFormattedTextField textComponent, final Value<LocalTime> value,
                                        final String dateFormat) {
    localTimeValueLink(textComponent, value, dateFormat, true);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param dateFormat the data format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   */
  public static void localTimeValueLink(final JFormattedTextField textComponent, final Value<LocalTime> value,
                                        final String dateFormat, final boolean updateOnKeystroke) {
    value.link(localTimeValue(textComponent, dateFormat, updateOnKeystroke));
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDate, JFormattedTextField> localDateValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat) {
    return localDateValue(textComponent, dateFormat, true);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDate, JFormattedTextField> localDateValue(final JFormattedTextField textComponent,
                                                                              final String dateFormat,
                                                                              final boolean updateOnKeystroke) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOnKeystroke, LocalDate::parse);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param dateFormat the data format
   */
  public static void localDateValueLink(final JFormattedTextField textComponent, final Value<LocalDate> value,
                                        final String dateFormat) {
    localDateValueLink(textComponent, value, dateFormat, true);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param dateFormat the data format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   */
  public static void localDateValueLink(final JFormattedTextField textComponent, final Value<LocalDate> value,
                                        final String dateFormat, final boolean updateOnKeystroke) {
    value.link(localDateValue(textComponent, dateFormat, updateOnKeystroke));
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDateTime, JFormattedTextField> localDateTimeValue(final JFormattedTextField textComponent,
                                                                                      final String dateFormat) {
    return localDateTimeValue(textComponent, dateFormat, true);
  }

  /**
   * @param textComponent the component
   * @param dateFormat the date format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   * @return a Value bound to the given component
   */
  public static ComponentValue<LocalDateTime, JFormattedTextField> localDateTimeValue(final JFormattedTextField textComponent,
                                                                                      final String dateFormat,
                                                                                      final boolean updateOnKeystroke) {
    return new TemporalFieldValue<>(textComponent, dateFormat, updateOnKeystroke, LocalDateTime::parse);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param dateFormat the data format
   */
  public static void localDateTimeValueLink(final JFormattedTextField textComponent, final Value<LocalDateTime> value,
                                            final String dateFormat) {
    localDateTimeValueLink(textComponent, value, dateFormat, true);
  }

  /**
   * Links a date value with a given text component
   * @param textComponent the text component to link with the value
   * @param value the model value
   * @param dateFormat the data format
   * @param updateOnKeystroke if true then the value is updated on each keystroke, otherwise on focus lost
   */
  public static void localDateTimeValueLink(final JFormattedTextField textComponent, final Value<LocalDateTime> value,
                                            final String dateFormat, final boolean updateOnKeystroke) {
    value.link(localDateTimeValue(textComponent, dateFormat, updateOnKeystroke));
  }
}
