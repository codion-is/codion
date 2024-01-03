/*
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.Action;
import javax.swing.JMenuItem;
import javax.swing.SwingConstants;

class DefaultMenuItemBuilder<C extends JMenuItem, B extends MenuItemBuilder<C, B>> extends AbstractButtonBuilder<Void, C, B>
        implements MenuItemBuilder<C, B> {

  DefaultMenuItemBuilder(Action action) {
    super(null);
    action(action);
    horizontalAlignment(SwingConstants.LEADING);
  }

  @Override
  protected C createButton() {
    return (C) new JMenuItem();
  }

  @Override
  protected final ComponentValue<Void, C> createComponentValue(C component) {
    return new AbstractComponentValue<Void, C>(component) {
      @Override
      protected Void getComponentValue() {
        return null;
      }

      @Override
      protected void setComponentValue(Void value) {/*Not applicable*/}
    };
  }

  @Override
  protected final void setInitialValue(C component, Void initialValue) {/*Not applicable*/}
}
