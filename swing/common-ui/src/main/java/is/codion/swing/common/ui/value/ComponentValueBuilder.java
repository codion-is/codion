/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JComponent;

/**
 * A builder for Values based on a input component
 * @param <V> the value type
 * @param <C> the component type
 */
public interface ComponentValueBuilder<V, C extends JComponent> {

  /**
   * @param component the component to base this value on
   * @return this builder instace
   */
  ComponentValueBuilder<V, C> component(C component);

  /**
   * @param initialValue the initial value
   * @return this builder instace
   */
  ComponentValueBuilder<V, C>  initalValue(V initialValue);

  /**
   * @return a ComponentValue
   */
  ComponentValue<V, C> build();
}
