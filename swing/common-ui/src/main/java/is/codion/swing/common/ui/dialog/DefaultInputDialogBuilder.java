/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.swing.common.ui.component.ComponentValue;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.BorderFactory;
import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static java.util.Objects.requireNonNull;

final class DefaultInputDialogBuilder<T> implements InputDialogBuilder<T> {

  private final ComponentValue<T, ?> componentValue;

  private JComponent owner;
  private String title;

  DefaultInputDialogBuilder(ComponentValue<T, ?> componentValue) {
    this.componentValue = requireNonNull(componentValue);
  }

  @Override
  public InputDialogBuilder<T> owner(JComponent owner) {
    this.owner = owner;
    return this;
  }

  @Override
  public InputDialogBuilder<T> title(String title) {
    this.title = title;
    return this;
  }

  @Override
  public T show() {
    State okPressed = State.state();
    JPanel basePanel = new JPanel(Layouts.borderLayout());
    basePanel.add(componentValue.component(), BorderLayout.CENTER);
    basePanel.setBorder(BorderFactory.createEmptyBorder(5, 5, 0, 5));
    new DefaultOkCancelDialogBuilder(basePanel)
            .owner(owner)
            .title(title)
            .onOk(() -> okPressed.set(true))
            .show();
    if (okPressed.get()) {
      return componentValue.get();
    }

    throw new CancelException();
  }
}
