/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;

import static java.util.Objects.requireNonNull;

final class DefaultOkCancelDialogBuilder extends AbstractDialogBuilder<OkCancelDialogBuilder> implements OkCancelDialogBuilder {

  private final JComponent component;

  private Action okAction;
  private Action cancelAction;

  DefaultOkCancelDialogBuilder(final JComponent component) {
    this.component = requireNonNull(component);
  }

  @Override
  public OkCancelDialogBuilder okAction(final Action okAction) {
    this.okAction = requireNonNull(okAction);
    return this;
  }

  @Override
  public OkCancelDialogBuilder cancelAction(final Action cancelAction) {
    this.cancelAction = requireNonNull(cancelAction);
    return this;
  }

  @Override
  public JDialog show() {
    final JDialog dialog = build();
    dialog.setVisible(true);

    return dialog;
  }

  @Override
  public JDialog build() {
    if (component == null) {
      throw new IllegalStateException("A component to display in the dialog must be specified");
    }
    if (okAction == null) {
      throw new IllegalStateException("okAction must be specified");
    }

    final JDialog dialog = new JDialog(owner, title);
    if (icon != null) {
      dialog.setIconImage(icon.getImage());
    }
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(component, BorderLayout.CENTER);

    final Action theCancelAction = cancelAction == null ? Control.control(dialog::dispose) : cancelAction;

    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .action(theCancelAction)
            .enable(dialog.getRootPane());
    KeyEvents.builder()
            .keyEvent(KeyEvent.VK_ENTER).condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .onKeyPressed()
            .action(okAction)
            .enable(dialog.getRootPane());
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(component, BorderLayout.CENTER);
    final JPanel buttonBasePanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    buttonBasePanel.add(Components.createOkCancelButtonPanel(okAction, theCancelAction));
    dialog.add(buttonBasePanel, BorderLayout.SOUTH);
    dialog.pack();
    if (dialog.getOwner() != null) {
      dialog.setLocationRelativeTo(dialog.getOwner());
    }
    dialog.setModal(true);
    dialog.setResizable(true);

    return dialog;
  }
}
