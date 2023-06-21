/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.button;

import is.codion.swing.common.ui.component.value.AbstractComponentValue;
import is.codion.swing.common.ui.component.value.ComponentValue;

import javax.swing.Action;
import javax.swing.JButton;

final class DefaultButtonBuilder<B extends ButtonBuilder<Void, JButton, B>> extends AbstractButtonBuilder<Void, JButton, B>
        implements ButtonBuilder<Void, JButton, B> {

  DefaultButtonBuilder(Action action) {
    super(null);
    action(action);
  }

  @Override
  protected JButton createButton() {
    return new JButton();
  }

  @Override
  protected ComponentValue<Void, JButton> createComponentValue(JButton component) {
    return new AbstractComponentValue<Void, JButton>(component) {
      @Override
      protected Void getComponentValue() {
        return null;
      }

      @Override
      protected void setComponentValue(Void value) {/*Not applicable*/}
    };
  }

  @Override
  protected void setInitialValue(JButton component, Void initialValue) {/*Not applicable*/}
}
