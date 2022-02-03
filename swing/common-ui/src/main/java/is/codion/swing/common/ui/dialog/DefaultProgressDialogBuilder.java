/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.control.Controls;

import javax.swing.JPanel;
import java.awt.Dimension;

class DefaultProgressDialogBuilder extends AbstractDialogBuilder<ProgressDialog.Builder> implements ProgressDialog.Builder {

  private boolean indeterminate = true;
  private boolean stringPainted = false;
  private JPanel northPanel;
  private JPanel westPanel;
  private Controls controls;
  private Dimension progressBarSize;

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
  public ProgressDialog.Builder controls(final Controls controls) {
    this.controls = controls;
    return this;
  }

  @Override
  public ProgressDialog.Builder progressBarSize(final Dimension progressBarSize) {
    this.progressBarSize = progressBarSize;
    return this;
  }

  @Override
  public ProgressDialog build() {
    return new ProgressDialog(owner, title, icon, indeterminate, stringPainted, northPanel,
            westPanel, controls, progressBarSize);
  }
}
