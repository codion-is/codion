/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.dialog.Dialogs.ProgressDialogBuilder;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.Window;

class DefaultProgressDialogBuilder implements ProgressDialogBuilder {

  private Window owner;
  private String title;
  private boolean indeterminate = true;
  private JPanel northPanel;
  private JPanel westPanel;
  private Controls buttonControls;

  @Override
  public ProgressDialogBuilder owner(final Window owner) {
    this.owner = owner;
    return this;
  }

  @Override
  public ProgressDialogBuilder dialogParent(final JComponent dialogParent) {
    if (owner != null) {
      throw new IllegalStateException("owner has alrady been set");
    }
    this.owner = dialogParent == null ? null : Windows.getParentWindow(dialogParent);
    return this;
  }

  @Override
  public ProgressDialogBuilder title(final String title) {
    this.title = title;
    return this;
  }

  @Override
  public ProgressDialogBuilder indeterminate(final boolean indeterminate) {
    this.indeterminate = indeterminate;
    return this;
  }

  @Override
  public ProgressDialogBuilder northPanel(final JPanel northPanel) {
    this.northPanel = northPanel;
    return this;
  }

  @Override
  public ProgressDialogBuilder westPanel(final JPanel westPanel) {
    this.westPanel = westPanel;
    return this;
  }

  @Override
  public ProgressDialogBuilder buttonControls(final Controls buttonControls) {
    this.buttonControls = buttonControls;
    return this;
  }

  @Override
  public ProgressDialog build() {
    return new ProgressDialog(owner, title, indeterminate, northPanel, westPanel, buttonControls);
  }
}
