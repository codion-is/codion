/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.Configuration;
import is.codion.common.value.PropertyValue;

import javax.swing.Action;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
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
   * Specifies the default number of columns in text fields created by component builders<br>
   * Value type: Integer<br>
   * Default value: 12
   */
  PropertyValue<Integer> DEFAULT_TEXT_FIELD_COLUMNS = Configuration.integerValue(
          "is.codion.swing.common.ui.TextFieldBuilder.defaultTextFieldColumns", 12);

  /**
   * Note that this disables {@link #transferFocusOnEnter(boolean)}.
   * @param action the action to associate with the text field
   * @return this builder instance
   */
  B action(Action action);

  /**
   * Note that this disables {@link #transferFocusOnEnter(boolean)}.
   * @param actionListener the action listener
   * @return this builder instance
   */
  B actionListener(ActionListener actionListener);

  /**
   * Makes the text field select all when it gains focus
   * @param selectAllOnFocusGained if true the component will select contents on focus gained
   * @return this builder instance
   */
  B selectAllOnFocusGained(final boolean selectAllOnFocusGained);

  /**
   * Adds a CTRL-SPACE action the given text field for displaying a lookup dialog showing the values provided
   * by the given value provider
   * @param valueSupplier provides the values for the lookup dialog
   * @return this builder instance
   */
  B lookupDialog(Supplier<Collection<T>> valueSupplier);

  /**
   * Associates the given format with the text field. Note that the format instance is
   * cloned on build, so format instances can be reused when calling this method.
   * @param format the format
   * @return this builder instance
   */
  B format(Format format);

  /**
   * @param horizontalAlignment the horizontal text alignment
   * @return this builder instance
   */
  B horizontalAlignment(int horizontalAlignment);
}
