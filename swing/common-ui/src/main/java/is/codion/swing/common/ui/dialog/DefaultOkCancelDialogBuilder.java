/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.panel.Panels;

import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.WindowConstants;
import javax.swing.border.Border;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.KeyEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.function.Consumer;

import static is.codion.swing.common.ui.dialog.DefaultComponentDialogBuilder.createDialog;
import static java.util.Objects.requireNonNull;

final class DefaultOkCancelDialogBuilder extends AbstractDialogBuilder<OkCancelDialogBuilder> implements OkCancelDialogBuilder {

  private final JComponent component;

  private boolean modal = true;
  private boolean resizable = true;
  private Dimension size;
  private JComponent locationRelativeTo;
  private Consumer<JDialog> onShown;
  private Action okAction;
  private Action cancelAction;
  private int buttonPanelConstraints = FlowLayout.RIGHT;
  private Border buttonPanelBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);

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
  public OkCancelDialogBuilder modal(final boolean modal) {
    this.modal = modal;
    return this;
  }

  @Override
  public OkCancelDialogBuilder resizable(final boolean resizable) {
    this.resizable = resizable;
    return this;
  }

  @Override
  public OkCancelDialogBuilder size(final Dimension size) {
    this.size = requireNonNull(size);
    return this;
  }

  @Override
  public OkCancelDialogBuilder buttonPanelConstraints(final int buttonPanelConstraints) {
    this.buttonPanelConstraints = buttonPanelConstraints;
    return this;
  }

  @Override
  public OkCancelDialogBuilder buttonPanelBorder(final Border buttonPanelBorder) {
    this.buttonPanelBorder = buttonPanelBorder;
    return this;
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
  public OkCancelDialogBuilder locationRelativeTo(final JComponent locationRelativeTo) {
    this.locationRelativeTo = requireNonNull(locationRelativeTo);
    return this;
  }

  @Override
  public OkCancelDialogBuilder onShown(final Consumer<JDialog> onShown) {
    this.onShown = onShown;
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
    final JButton okButton = new JButton(okAction);
    final JButton cancelButton = new JButton(cancelAction);
    okButton.setText(Messages.get(Messages.OK));
    okButton.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));
    cancelButton.setText(Messages.get(Messages.CANCEL));
    cancelButton.setMnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0));

    final JPanel panel = new JPanel(new BorderLayout());
    panel.add(component, BorderLayout.CENTER);
    panel.add(createButtonBasePanel(okButton, cancelButton), BorderLayout.SOUTH);

    final JDialog dialog = createDialog(owner, title, icon, panel, size, locationRelativeTo, modal, resizable, onShown);
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(new WindowAdapter() {
      @Override
      public void windowClosing(final WindowEvent e) {
        cancelAction.actionPerformed(null);
      }
    });
    dialog.getRootPane().setDefaultButton(okButton);
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .action(cancelAction)
            .enable(dialog.getRootPane());

    return dialog;
  }

  private Control.Builder performAndCloseControl(final Control.Command command) {
    return Control.builder(() -> {
      command.perform();
      Windows.getParentDialog(component).dispose();
    });
  }

  private JPanel createButtonBasePanel(final JButton okButton, final JButton cancelButton) {
    return Panels.builder(new FlowLayout(buttonPanelConstraints))
            .add(Panels.builder(Layouts.gridLayout(1, 2))
                    .add(okButton)
                    .add(cancelButton)
                    .build())
            .border(buttonPanelBorder)
            .build();
  }
}
