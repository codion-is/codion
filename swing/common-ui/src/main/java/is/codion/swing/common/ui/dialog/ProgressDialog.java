/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.ControlList;
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
                        final JPanel northPanel, final ControlList buttonControls) {
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
  protected void initializeUI(final JPanel northPanel, final ControlList buttonControls) {
    setLayout(Layouts.borderLayout());
    if (northPanel != null) {
      add(northPanel, BorderLayout.NORTH);
    }
    add(progressBar, BorderLayout.CENTER);
    if (buttonControls != null) {
      final JPanel southPanel = new JPanel(Layouts.flowLayout(FlowLayout.TRAILING));
      southPanel.add(initializeButtonPanel(buttonControls));
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

  private static JPanel initializeButtonPanel(final ControlList buttonControls) {
    return Controls.createHorizontalButtonPanel(buttonControls);
  }
}
