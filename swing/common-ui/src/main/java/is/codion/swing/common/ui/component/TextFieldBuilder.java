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
 */
public interface TextFieldBuilder<T> extends TextComponentBuilder<T, JTextField, TextFieldBuilder<T>> {

  /**
   * Note that this disables {@link #transferFocusOnEnter(boolean)}.
   * @param action the action to associate with the text field
   * @return this builder instance
   */
  TextFieldBuilder<T> action(Action action);

  /**
   * Makes the text field select all when it gains focus
   * @return this builder instance
   */
  TextFieldBuilder<T> selectAllOnFocusGained();

  /**
   * Adds a CTRL-SPACE action the the given text field for displaying a lookup dialog showing the values provided
   * by the given value provider
   * @param valueSupplier provides the values for the lookup dialog
   * @return this builder instance
   */
  TextFieldBuilder<T> lookupDialog(Supplier<Collection<T>> valueSupplier);

  /**
   * @param format the format
   * @return this builder instance
   */
  TextFieldBuilder<T> format(Format format);
}
