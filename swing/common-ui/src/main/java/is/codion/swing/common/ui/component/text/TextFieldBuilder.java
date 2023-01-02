/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.text;

import is.codion.common.Configuration;
import is.codion.common.properties.PropertyValue;
import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.SelectionProvider;

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
   * Specifies the default number of columns in text fields created by component builders<br>
   * Value type: Integer<br>
   * Default value: 12
   */
  PropertyValue<Integer> DEFAULT_TEXT_FIELD_COLUMNS =
          Configuration.integerValue("is.codion.swing.common.ui.TextFieldBuilder.defaultTextFieldColumns", 12);

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
   * Makes the text field select all when it gains focus
   * @param selectAllOnFocusGained if true the component will select contents on focus gained
   * @return this builder instance
   */
  B selectAllOnFocusGained(boolean selectAllOnFocusGained);

  /**
   * Adds a CTRL-SPACE action the given text field allowing the user to select a value to display in the field
   * @param selectionProvider the selection provider
   * @return this builder instance
   */
  B selectionProvider(SelectionProvider<T> selectionProvider);

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
