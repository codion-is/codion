/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.control.Controls;

import javax.swing.JPanel;

class DefaultProgressDialogBuilder extends AbstractDialogBuilder<ProgressDialog.Builder> implements ProgressDialog.Builder {

  private boolean indeterminate = true;
  private boolean stringPainted = false;
  private JPanel northPanel;
  private JPanel westPanel;
  private Controls buttonControls;

  @Override
  public ProgressDialog.Builder indeterminate(final boolean indeterminate) {
    this.indeterminate = indeterminate;
    return this;
  }

  @Override
  public ProgressDialog.Builder stringPainted(final boolean stringPainted) {
    this.stringPainted = stringPainted;
    return this;
  }

  @Override
  public ProgressDialog.Builder northPanel(final JPanel northPanel) {
    this.northPanel = northPanel;
    return this;
  }

  @Override
  public ProgressDialog.Builder westPanel(final JPanel westPanel) {
    this.westPanel = westPanel;
    return this;
  }

  @Override
  public ProgressDialog.Builder buttonControls(final Controls buttonControls) {
    this.buttonControls = buttonControls;
    return this;
  }

  @Override
  public ProgressDialog build() {
    return new ProgressDialog(owner, title, icon, indeterminate, stringPainted, northPanel, westPanel, buttonControls);
  }
}
