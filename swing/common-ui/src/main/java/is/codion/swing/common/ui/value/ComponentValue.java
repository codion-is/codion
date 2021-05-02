/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import is.codion.common.value.Value;

import javax.swing.JComponent;

/**
 * A {@link Value} represented by an input component of some sort.<br>
 * {@link ComponentValues} is a factory for {@link ComponentValue} implementations.
 * @param <V> the value type
 * @param <C> the component type
 */
public interface ComponentValue<V, C extends JComponent> extends Value<V> {

  /**
   * @return the input component representing the value
   */
  C getComponent();

  /**
   * A builder for Values based on an input component
   * @param <V> the value type
   * @param <C> the component type
   */
  interface Builder<V, C extends JComponent> {

    /**
     * @param component the component to base this value on
     * @return this builder instace
     */
    Builder<V, C> component(C component);

    /**
     * @param initialValue the initial value
     * @return this builder instace
     */
    Builder<V, C> initalValue(V initialValue);

    /**
     * @return a ComponentValue
     */
    ComponentValue<V, C> build();
  }
}
