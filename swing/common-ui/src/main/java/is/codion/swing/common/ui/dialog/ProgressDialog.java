/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.Layouts;

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
public final class ProgressDialog extends JDialog {

  public static final int DEFAULT_PROGRESS_BAR_WIDTH = 400;

  private final JProgressBar progressBar;

  ProgressDialog(final Window dialogOwner, final String title, final int maxProgress,
                 final JPanel northPanel, final JPanel westPanel, final Controls buttonControls) {
    super(dialogOwner, ModalityType.APPLICATION_MODAL);
    setTitle(title);
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    progressBar = initializeProgressBar(maxProgress);
    initializeUI(northPanel, westPanel, buttonControls);
  }

  /**
   * @return the progress bar model
   */
  public BoundedRangeModel getProgressModel() {
    return progressBar.getModel();
  }

  /**
   * Initalizes the UI, override for a custom look
   * @param northPanel a panel to display at the {@link BorderLayout#NORTH} position
   * @param westPanel a panel to display at the {@link BorderLayout#WEST} position
   * @param buttonControls if specified buttons based on these controls are added to this dialog
   */
  private void initializeUI(final JPanel northPanel, final JPanel westPanel, final Controls buttonControls) {
    setLayout(Layouts.borderLayout());
    if (northPanel != null) {
      add(northPanel, BorderLayout.NORTH);
    }
    if (westPanel != null) {
      add(westPanel, BorderLayout.WEST);
    }
    add(progressBar, BorderLayout.CENTER);
    if (buttonControls != null) {
      final JPanel southPanel = new JPanel(Layouts.flowLayout(FlowLayout.TRAILING));
      southPanel.add(buttonControls.createHorizontalButtonPanel());
      add(southPanel, BorderLayout.SOUTH);
    }
    pack();
    Windows.centerWindow(this);
  }

  private static JProgressBar initializeProgressBar(final int maxProgress) {
    final JProgressBar bar = new JProgressBar();
    Components.setPreferredWidth(bar, DEFAULT_PROGRESS_BAR_WIDTH);
    if (maxProgress < 0) {
      bar.setIndeterminate(true);
    }
    else {
      bar.setMaximum(maxProgress);
    }

    return bar;
  }
}
