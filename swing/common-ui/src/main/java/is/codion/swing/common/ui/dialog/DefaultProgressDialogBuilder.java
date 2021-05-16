/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.control.Controls;

import javax.swing.JPanel;

class DefaultProgressDialogBuilder extends AbstractDialogBuilder<ProgressDialogBuilder> implements ProgressDialogBuilder {

  private boolean indeterminate = true;
  private JPanel northPanel;
  private JPanel westPanel;
  private Controls buttonControls;

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
    return new ProgressDialog(owner, title, icon, indeterminate, northPanel, westPanel, buttonControls);
  }
}
