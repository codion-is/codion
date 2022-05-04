/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.swing.common.ui.KeyEvents;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.layout.Layouts;

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

import static java.util.Objects.requireNonNull;

final class DefaultOkCancelDialogBuilder extends AbstractDialogBuilder<OkCancelDialogBuilder> implements OkCancelDialogBuilder {

  private final JComponent component;

  private boolean modal = true;
  private boolean resizable = true;
  private Dimension size;
  private Consumer<JDialog> onShown;
  private Action okAction;
  private Action cancelAction;
  private int buttonPanelConstraints = FlowLayout.RIGHT;
  private Border buttonPanelBorder = BorderFactory.createEmptyBorder(10, 10, 10, 10);

  DefaultOkCancelDialogBuilder(JComponent component) {
    this.component = requireNonNull(component);
    this.okAction = Control.control(new DefaultOkCommand(component));
    this.cancelAction = Control.control(new DefaultCancelCommand(component));
  }

  @Override
  public OkCancelDialogBuilder modal(boolean modal) {
    this.modal = modal;
    return this;
  }

  @Override
  public OkCancelDialogBuilder resizable(boolean resizable) {
    this.resizable = resizable;
    return this;
  }

  @Override
  public OkCancelDialogBuilder size(Dimension size) {
    this.size = requireNonNull(size);
    return this;
  }

  @Override
  public OkCancelDialogBuilder buttonPanelConstraints(int buttonPanelConstraints) {
    this.buttonPanelConstraints = buttonPanelConstraints;
    return this;
  }

  @Override
  public OkCancelDialogBuilder buttonPanelBorder(Border buttonPanelBorder) {
    this.buttonPanelBorder = buttonPanelBorder;
    return this;
  }

  @Override
  public OkCancelDialogBuilder onOk(Runnable onOk) {
    return okAction(performAndCloseControl(requireNonNull(onOk)));
  }

  @Override
  public OkCancelDialogBuilder onCancel(Runnable onCancel) {
    return cancelAction(performAndCloseControl(requireNonNull(onCancel)));
  }

  @Override
  public OkCancelDialogBuilder okAction(Action okAction) {
    this.okAction = requireNonNull(okAction);
    return this;
  }

  @Override
  public OkCancelDialogBuilder cancelAction(Action cancelAction) {
    this.cancelAction = requireNonNull(cancelAction);
    return this;
  }

  @Override
  public OkCancelDialogBuilder onShown(Consumer<JDialog> onShown) {
    this.onShown = onShown;
    return this;
  }

  @Override
  public JDialog show() {
    JDialog dialog = build();
    dialog.setVisible(true);

    return dialog;
  }

  @Override
  public JDialog build() {
    JButton okButton = new JButton(okAction);
    okButton.setText(Messages.get(Messages.OK));
    okButton.setMnemonic(Messages.get(Messages.OK_MNEMONIC).charAt(0));
    JButton cancelButton = new JButton(cancelAction);
    cancelButton.setText(Messages.get(Messages.CANCEL));
    cancelButton.setMnemonic(Messages.get(Messages.CANCEL_MNEMONIC).charAt(0));
    JPanel buttonPanel = new JPanel(Layouts.gridLayout(1, 2));
    buttonPanel.add(okButton);
    buttonPanel.add(cancelButton);
    JPanel buttonBasePanel = new JPanel(Layouts.flowLayout(buttonPanelConstraints));
    buttonBasePanel.add(buttonPanel, BorderLayout.SOUTH);
    buttonBasePanel.setBorder(buttonPanelBorder);
    JPanel panel = new JPanel(new BorderLayout());
    panel.add(component, BorderLayout.CENTER);
    panel.add(buttonBasePanel, BorderLayout.SOUTH);

    JDialog dialog = DefaultComponentDialogBuilder.createDialog(owner, titleProvider, icon, panel, size, locationRelativeTo, modal, resizable, onShown);
    dialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    dialog.addWindowListener(new CancelOnWindowClosingListener(cancelAction));
    dialog.getRootPane().setDefaultButton(okButton);
    KeyEvents.builder(KeyEvent.VK_ESCAPE)
            .condition(JComponent.WHEN_IN_FOCUSED_WINDOW)
            .action(cancelAction)
            .enable(dialog.getRootPane());

    return dialog;
  }

  private Control performAndCloseControl(Runnable command) {
    return Control.control(new PerformAndCloseCommand(command, component));
  }

  private static final class CancelOnWindowClosingListener extends WindowAdapter {

    private final Action cancelAction;

    private CancelOnWindowClosingListener(Action cancelAction) {
      this.cancelAction = cancelAction;
    }

    @Override
    public void windowClosing(WindowEvent e) {
      cancelAction.actionPerformed(null);
    }
  }

  private static final class PerformAndCloseCommand implements Control.Command {

    private final Runnable command;
    private final JComponent component;

    private PerformAndCloseCommand(Runnable command, JComponent component) {
      this.command = command;
      this.component = component;
    }

    @Override
    public void perform() throws Exception {
      command.run();
      Windows.getParentDialog(component).ifPresent(JDialog::dispose);
    }
  }

  private static final class DefaultOkCommand implements Control.Command {

    private final JComponent component;

    private DefaultOkCommand(JComponent component) {
      this.component = component;
    }

    @Override
    public void perform() throws Exception {
      Windows.getParentDialog(component).ifPresent(JDialog::dispose);
    }
  }

  private static final class DefaultCancelCommand implements Control.Command {

    private final JComponent component;

    private DefaultCancelCommand(JComponent component) {
      this.component = component;
    }

    @Override
    public void perform() throws Exception {
      Windows.getParentDialog(component).ifPresent(JDialog::dispose);
    }
  }
}
