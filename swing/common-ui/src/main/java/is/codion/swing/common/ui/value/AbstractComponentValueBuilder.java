/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.value;

import javax.swing.JComponent;

import static java.util.Objects.requireNonNull;

abstract class AbstractComponentValueBuilder<V, C extends JComponent> implements ComponentValueBuilder<V, C> {

  protected C component;
  protected V initialValue;

  @Override
  public ComponentValueBuilder<V, C> component(final C component) {
    this.component = requireNonNull(component);
    return this;
  }

  @Override
  public ComponentValueBuilder<V, C> initalValue(final V initialValue) {
    this.initialValue = initialValue;
    return this;
  }
}
