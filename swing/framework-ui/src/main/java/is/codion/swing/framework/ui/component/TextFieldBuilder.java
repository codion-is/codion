/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.framework.ui.component;

import is.codion.swing.common.ui.value.UpdateOn;

import javax.swing.Action;
import javax.swing.JTextField;

/**
 * Builds a JTextField.
 * @param <T> the type the text field represents
 */
public interface TextFieldBuilder<T> extends ComponentBuilder<T, JTextField, TextFieldBuilder<T>> {

  /**
   * @param editable false if the field should not be editable
   * @return this builder instance
   */
  TextFieldBuilder<T> editable(boolean editable);

  /**
   * @param updateOn specifies when the underlying value should be updated
   * @return this builder instance
   */
  TextFieldBuilder<T> updateOn(UpdateOn updateOn);

  /**
   * @param columns the number of colums in the text field
   * @return this builder instance
   */
  TextFieldBuilder<T> columns(int columns);

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
   * Makes the text field convert all lower case input to upper case
   * @return this builder instance
   */
  TextFieldBuilder<T> upperCase();

  /**
   * Makes the text field convert all upper case input to lower case
   * @return this builder instance
   */
  TextFieldBuilder<T> lowerCase();
}
