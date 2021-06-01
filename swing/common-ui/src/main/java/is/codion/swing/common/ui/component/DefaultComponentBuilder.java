/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.value.ComponentValue;

import javax.swing.JComponent;

final class DefaultComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>> extends AbstractComponentBuilder<T, C, B> {

  private final C component;

  DefaultComponentBuilder(final C component) {
    this.component = component;
  }

  @Override
  protected C buildComponent() {
    return component;
  }

  @Override
  protected ComponentValue<T, C> buildComponentValue(final C component) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void setInitialValue(final C component, final T initialValue) {}
}
