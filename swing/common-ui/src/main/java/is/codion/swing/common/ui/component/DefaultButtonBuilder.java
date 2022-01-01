/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import javax.swing.Action;
import javax.swing.JButton;

final class DefaultButtonBuilder<B extends ButtonBuilder<Void, JButton, B>> extends AbstractButtonBuilder<Void, JButton, B>
        implements ButtonBuilder<Void, JButton, B> {

  DefaultButtonBuilder(final Action action) {
    super(null);
    action(action);
  }

  @Override
  protected JButton createButton() {
    return new JButton();
  }

  @Override
  protected ComponentValue<Void, JButton> buildComponentValue(final JButton component) {
    return new AbstractComponentValue<Void, JButton>(component) {
      @Override
      protected Void getComponentValue(final JButton component) {
        return null;
      }

      @Override
      protected void setComponentValue(final JButton component, final Void value) {/*Not applicable*/}
    };
  }

  @Override
  protected void setInitialValue(final JButton component, final Void initialValue) {/*Not applicable*/}
}
