/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.dialog.SelectionDialogBuilder.Selector;

import javax.swing.Action;
import javax.swing.JTextField;
import java.awt.event.ActionListener;
import java.text.Format;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JTextField.
 * @param <T> the type the text field represents
 * @param <C> the text field type
 * @param <B> the builder type
 */
public interface TextFieldBuilder<T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> extends TextComponentBuilder<T, C, B> {

  /**
   * @param columns the number of colums in the text component
   * @return this builder instance
   * @see JTextField#setColumns(int)
   */
  B columns(int columns);

  /**
   * Note that this disables {@link #transferFocusOnEnter(boolean)}.
   * @param action the action to associate with the text field
   * @return this builder instance
   * @see JTextField#setAction(Action)
   */
  B action(Action action);

  /**
   * Note that this disables {@link #transferFocusOnEnter(boolean)}.
   * @param actionListener the action listener
   * @return this builder instance
   * @see JTextField#addActionListener(ActionListener)
   */
  B actionListener(ActionListener actionListener);

  /**
   * Adds a CTRL-SPACE action the given text field allowing the user to select a value to display in the field
   * @param selector the selector providing the values to select from
   * @return this builder instance
   */
  B selector(Selector<T> selector);

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
   * @see JTextField#setHorizontalAlignment(int)
   */
  B horizontalAlignment(int horizontalAlignment);

  /**
   * @param hintText the hint text to display when the field is empty and unfocused
   * @return this builder instance
   */
  B hintText(String hintText);

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @return a builder for a component
   */
  static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> builder(Class<T> valueClass) {
    return new DefaultTextFieldBuilder<>(valueClass, null);
  }

  /**
   * @param <T> the value type
   * @param <C> the text field type
   * @param <B> the builder type
   * @param valueClass the value class
   * @param linkedValue the value to link to the component
   * @return a builder for a component
   */
  static <T, C extends JTextField, B extends TextFieldBuilder<T, C, B>> TextFieldBuilder<T, C, B> builder(Class<T> valueClass,
                                                                                                          Value<T> linkedValue) {
    return new DefaultTextFieldBuilder<>(valueClass, requireNonNull(linkedValue));
  }
}
