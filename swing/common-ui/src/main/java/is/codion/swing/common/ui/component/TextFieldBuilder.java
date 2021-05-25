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
 * @param <B> the builder type
 */
public interface TextFieldBuilder<T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> extends TextComponentBuilder<T, C, B> {

  /**
   * Note that this disables {@link #transferFocusOnEnter(boolean)}.
   * @param action the action to associate with the text field
   * @return this builder instance
   */
  B action(Action action);

  /**
   * Makes the text field select all when it gains focus
   * @return this builder instance
   */
  B selectAllOnFocusGained();

  /**
   * Adds a CTRL-SPACE action the the given text field for displaying a lookup dialog showing the values provided
   * by the given value provider
   * @param valueSupplier provides the values for the lookup dialog
   * @return this builder instance
   */
  B lookupDialog(Supplier<Collection<T>> valueSupplier);

  /**
   * @param format the format
   * @return this builder instance
   */
  B format(Format format);

  /**
   * @param dateTimePattern the date time pattern, if applicable
   * @return this builder instance
   */
  B dateTimePattern(String dateTimePattern);
}
