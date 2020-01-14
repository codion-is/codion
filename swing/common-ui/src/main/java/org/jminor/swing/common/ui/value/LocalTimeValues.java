/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.value;

import org.jminor.common.value.Value;
import org.jminor.common.value.Values;

import javax.swing.JFormattedTextField;
import java.time.LocalTime;

public final class LocalTimeValues {

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
    Values.link(value, localTimeValue(textComponent, dateFormat, updateOnKeystroke));
  }
}
