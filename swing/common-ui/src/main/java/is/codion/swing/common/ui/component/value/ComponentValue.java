/*
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.value;

import is.codion.common.value.Value;
import is.codion.swing.common.ui.component.Components;

import javax.swing.JComponent;

/**
 * A {@link Value} represented by an input component of some sort.<br>
 * {@link Components} is a factory for {@link ComponentValue} implementations.
 * @param <T> the value type
 * @param <C> the component type
 */
public interface ComponentValue<T, C extends JComponent> extends Value<T> {

  /**
   * @return the input component representing the value
   */
  C component();
}
