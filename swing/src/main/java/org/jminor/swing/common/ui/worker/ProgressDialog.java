/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.worker;

import org.jminor.swing.common.ui.UiUtil;

import javax.swing.BoundedRangeModel;
import javax.swing.JDialog;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import java.awt.Window;

/**
 * A dialog containing a progress bar.
 */
public class ProgressDialog extends JDialog {

  public static final int DEFAULT_PROGRESS_BAR_WIDTH = 400;

  private final JProgressBar progressBar;

  /**
   * Instantiates a new 'inditerminate' progress bar dialog.
   * @param dialogOwner the dialog owner
   * @param title the title
   */
  public ProgressDialog(final Window dialogOwner, final String title) {
    this(dialogOwner, title, -1);
  }

  /**
   * Instantiates a new progress bar dialog.
   * @param dialogOwner the dialog owner
   * @param title the title
   * @param maxProgress the maximum progress for the progress bar, -1 for indeterminate,
   */
  public ProgressDialog(final Window dialogOwner, final String title, final int maxProgress) {
    super(dialogOwner, ModalityType.APPLICATION_MODAL);
    setTitle(title);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    progressBar = initializeProgressBar(maxProgress);
    initializeUI();
  }

  public final BoundedRangeModel getProgressModel() {
    return progressBar.getModel();
  }

  /**
   * Initalizes the UI, override for a custom look
   */
  protected void initializeUI() {
    add(getProgressBar());
    pack();
    UiUtil.centerWindow(this);
  }

  protected final JProgressBar getProgressBar() {
    return progressBar;
  }

  private JProgressBar initializeProgressBar(final int maxProgress) {
    final JProgressBar progressBar = new JProgressBar();
    UiUtil.setPreferredWidth(progressBar, DEFAULT_PROGRESS_BAR_WIDTH);
    if (maxProgress < 0) {
      progressBar.setIndeterminate(true);
    }
    else {
      progressBar.setMaximum(maxProgress);
    }

    return progressBar;
  }
}
