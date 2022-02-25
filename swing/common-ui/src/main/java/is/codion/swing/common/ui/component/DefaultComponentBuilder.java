/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.JComponent;

final class DefaultComponentBuilder<T, C extends JComponent, B extends ComponentBuilder<T, C, B>> extends AbstractComponentBuilder<T, C, B> {

  private final C component;

  DefaultComponentBuilder(C component) {
    this.component = component;
  }

  @Override
  protected C buildComponent() {
    return component;
  }

  @Override
  protected ComponentValue<T, C> buildComponentValue(C component) {
    throw new UnsupportedOperationException();
  }

  @Override
  protected void setInitialValue(C component, T initialValue) {/*Not implemented*/}
}
