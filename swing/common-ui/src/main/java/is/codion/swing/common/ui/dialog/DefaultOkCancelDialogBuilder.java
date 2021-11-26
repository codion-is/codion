/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import static java.util.Objects.requireNonNull;

final class DefaultOkCancelDialogBuilder extends AbstractDialogBuilder<OkCancelDialogBuilder> implements OkCancelDialogBuilder {

  private final JComponent component;

  private Action okAction;
  private Action cancelAction;

  DefaultOkCancelDialogBuilder(final JComponent component) {
    this.component = requireNonNull(component);
    this.okAction = Control.builder(() -> Windows.getParentDialog(component).dispose())
            .caption(Messages.get(Messages.OK))
            .mnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0))
            .build();
    this.cancelAction = Control.builder(() -> Windows.getParentDialog(component).dispose())
            .caption(Messages.get(Messages.CANCEL))
            .mnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0))
            .build();
  }

  @Override
  public OkCancelDialogBuilder onOk(final Control.Command command) {
    return okAction(performAndCloseControl(requireNonNull(command))
            .caption(Messages.get(Messages.OK))
            .mnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0))
            .build());
  }

  @Override
  public OkCancelDialogBuilder onCancel(final Control.Command command) {
    return cancelAction(performAndCloseControl(requireNonNull(command))
            .caption(Messages.get(Messages.CANCEL))
            .mnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0))
            .build());
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
    final JDialog dialog = new JDialog(owner, title);
    if (icon != null) {
      dialog.setIconImage(icon.getImage());
    }
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(component, BorderLayout.CENTER);

    final Action theCancelAction = cancelAction == null ? Control.control(dialog::dispose) : cancelAction;

    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        theCancelAction.actionPerformed(null);
      }
    });
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .action(theCancelAction)
            .enable(dialog.getRootPane());
    KeyEvents.builder(KeyEvent.VK_ENTER).condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .onKeyPressed()
            .action(okAction)
            .enable(dialog.getRootPane());
    dialog.setLayout(Layouts.borderLayout());
    dialog.add(component, BorderLayout.CENTER);
    final JPanel buttonBasePanel = new JPanel(Layouts.flowLayout(FlowLayout.CENTER));
    buttonBasePanel.add(createOkCancelButtonPanel(okAction, theCancelAction));
    dialog.add(buttonBasePanel, BorderLayout.SOUTH);
    dialog.pack();
    dialog.setLocationRelativeTo(owner);
    dialog.setModal(true);
    dialog.setResizable(true);

    return dialog;
  }

  private Control.Builder performAndCloseControl(final Control.Command command) {
    return Control.builder(() -> {
      command.perform();
      Windows.getParentDialog(component).dispose();
    });
  }

  private static JPanel createOkCancelButtonPanel(final Action okAction, final Action cancelAction) {
    requireNonNull(okAction, "okAction");
    requireNonNull(cancelAction, "cancelAction");
    final JButton okButton = new JButton(okAction);
    final JButton cancelButton = new JButton(cancelAction);
    okButton.setText(Messages.get(Messages.OK));
    okButton.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));
    cancelButton.setText(Messages.get(Messages.CANCEL));
    cancelButton.setMnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0));
    final JPanel buttonPanel = new JPanel(Layouts.gridLayout(1, 2));
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);

    return buttonPanel;
  }
}
