/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.swing.common.ui.value;

import dev.codion.common.value.Value;

/**
 * A {@link Value} represented by an input component of some sort.
 * @param <V> the Value type
 * @param <C> the component type
 */
public interface ComponentValue<V, C> extends Value<V> {

  /**
   * @return the input component representing the value
   */
  C getComponent();
}
