/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.common.value.Value;

import javax.swing.JComponent;

/**
 * A {@link Value} represented by an input component of some sort.<br>
 * {@link ComponentValues} is a factory for {@link ComponentValue} implementations.
 * @param <T> the value type
 * @param <C> the component type
 */
public interface ComponentValue<T, C extends JComponent> extends Value<T> {

  /**
   * @return the input component representing the value
   */
  C getComponent();

  /**
   * Displays the underlying component in a dialog and returns the value if the user presses OK.
   * @param owner the dialog owner
   * @return the value from the underlying component if the user presses OK
   * @throws is.codion.common.model.CancelException if the user cancels
   */
  T showDialog(JComponent owner);

  /**
   * Displays the underlying component in a dialog and returns the value if the user presses OK.
   * @param owner the dialog owner
   * @param title the dialog title
   * @return the value from the underlying component if the user presses OK
   * @throws is.codion.common.model.CancelException if the user cancels
   */
  T showDialog(JComponent owner, String title);
}
