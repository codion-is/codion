/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.worker;

import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.control.ControlProvider;
import org.jminor.swing.common.ui.control.ControlSet;

import javax.swing.BoundedRangeModel;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
    this(dialogOwner, title, maxProgress, null, null);
  }

  /**
   * Instantiates a new progress bar dialog.
   * @param dialogOwner the dialog owner
   * @param title the title
   * @param maxProgress the maximum progress for the progress bar, -1 for indeterminate,
   * @param northPanel if specified this panel is added to the {@link BorderLayout#NORTH} position
   * @param buttonControls if specified buttons based on these controls are added to the {@link BorderLayout#SOUTH} position
   */
  public ProgressDialog(final Window dialogOwner, final String title, final int maxProgress,
                        final JPanel northPanel, final ControlSet buttonControls) {
    super(dialogOwner, ModalityType.APPLICATION_MODAL);
    setTitle(title);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    progressBar = initializeProgressBar(maxProgress);
    initializeUI(northPanel, buttonControls);
  }

  /**
   * @return the progress bar model
   */
  public final BoundedRangeModel getProgressModel() {
    return progressBar.getModel();
  }

  /**
   * Initalizes the UI, override for a custom look
   * @param northPanel a panel to display at the {@link BorderLayout#NORTH} position
   * @param buttonControls if specified buttons based on these controls are added to this dialog
   */
  protected void initializeUI(final JPanel northPanel, final ControlSet buttonControls) {
    setLayout(new BorderLayout(5, 5));
    if (northPanel != null) {
      add(northPanel, BorderLayout.NORTH);
    }
    add(getProgressBar(), BorderLayout.CENTER);
    if (buttonControls != null) {
      final JPanel southPanel = new JPanel(new FlowLayout(FlowLayout.TRAILING));
      southPanel.add(initializeButtonPanel(buttonControls));
      add(southPanel, BorderLayout.SOUTH);
    }
    pack();
    UiUtil.centerWindow(this);
  }

  protected final JProgressBar getProgressBar() {
    return progressBar;
  }

  private JProgressBar initializeProgressBar(final int maxProgress) {
    final JProgressBar bar = new JProgressBar();
    UiUtil.setPreferredWidth(bar, DEFAULT_PROGRESS_BAR_WIDTH);
    if (maxProgress < 0) {
      bar.setIndeterminate(true);
    }
    else {
      bar.setMaximum(maxProgress);
    }

    return bar;
  }

  private JPanel initializeButtonPanel(final ControlSet buttonControls) {
    return ControlProvider.createHorizontalButtonPanel(buttonControls);
  }
}
