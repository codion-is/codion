/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

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
      protected Void getComponentValue(JButton component) {
        return null;
      }

      @Override
      protected void setComponentValue(JButton component, Void value) {/*Not applicable*/}
    };
  }

  @Override
  protected void setInitialValue(JButton component, Void initialValue) {/*Not applicable*/}
}
