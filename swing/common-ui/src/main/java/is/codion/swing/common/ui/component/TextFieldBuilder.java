/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.Action;
import javax.swing.JTextField;
import java.text.Format;
import java.util.Collection;
import java.util.function.Supplier;

/**
 * Builds a JTextField.
 * @param <T> the type the text field represents
 * @param <C> the text field type
 */
public interface TextFieldBuilder<T, C extends JTextField> extends TextComponentBuilder<T, C, TextFieldBuilder<T, C>> {

  /**
   * Note that this disables {@link #transferFocusOnEnter(boolean)}.
   * @param action the action to associate with the text field
   * @return this builder instance
   */
  TextFieldBuilder<T, C> action(Action action);

  /**
   * Makes the text field select all when it gains focus
   * @return this builder instance
   */
  TextFieldBuilder<T, C> selectAllOnFocusGained();

  /**
   * Adds a CTRL-SPACE action the the given text field for displaying a lookup dialog showing the values provided
   * by the given value provider
   * @param valueSupplier provides the values for the lookup dialog
   * @return this builder instance
   */
  TextFieldBuilder<T, C> lookupDialog(Supplier<Collection<T>> valueSupplier);

  /**
   * @param format the format
   * @return this builder instance
   */
  TextFieldBuilder<T, C> format(Format format);

  /**
   * @param dateTimePattern the date time pattern, if applicable
   * @return this builder instance
   */
  TextFieldBuilder<T, C> dateTimePattern(String dateTimePattern);

  /**
   * @param minimumValue the minimum numerical value, if applicable
   * @return this builder instance
   */
  TextFieldBuilder<T, C> minimumValue(Double minimumValue);

  /**
   * @param maximumValue the maximum numerical value, if applicable
   * @return this builder instance
   */
  TextFieldBuilder<T, C> maximumValue(Double maximumValue);

  /**
   * @param maximumFractionDigits the maximum fraction digits for floating point numbers, if applicable
   * @return this builder instance
   */
  TextFieldBuilder<T, C> maximumFractionDigits(int maximumFractionDigits);
}
