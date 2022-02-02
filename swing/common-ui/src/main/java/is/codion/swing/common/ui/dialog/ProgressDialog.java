/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.ImageIcon;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * A dialog containing a progress bar.
 * @see Dialogs#progressDialog()
 */
public final class ProgressDialog extends JDialog {

  public static final int DEFAULT_PROGRESS_BAR_WIDTH = 400;

  private final JProgressBar progressBar;

  ProgressDialog(final Window dialogOwner, final String title, final ImageIcon icon, final boolean indeterminate,
                 final boolean stringPainted, final JPanel northPanel, final JPanel westPanel, final Controls buttonControls) {
    super(dialogOwner, ModalityType.APPLICATION_MODAL);
    setTitle(title);
    if (icon != null) {
      setIconImage(icon.getImage());
    }
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    progressBar = initializeProgressBar(indeterminate, stringPainted);
    initializeUI(northPanel, westPanel, buttonControls);
    setLocationRelativeTo(dialogOwner);
  }

  /**
   * Sets the progress in the underlying JProgressBar
   * @param progress the progress (0 - 100)
   */
  public void setProgress(final int progress) {
    progressBar.getModel().setValue(progress);
  }

  /**
   * Sets the message displayed on the JProgress bar
   * @param message the message
   */
  public void setMessage(final String message) {
    progressBar.setString(message);
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

  private static JProgressBar initializeProgressBar(final boolean indeterminate, final boolean stringPainted) {
    final JProgressBar progressBar = new JProgressBar();
    progressBar.setStringPainted(stringPainted);
    Sizes.setPreferredWidth(progressBar, DEFAULT_PROGRESS_BAR_WIDTH);
    if (indeterminate) {
      progressBar.setIndeterminate(true);
    }
    else {
      progressBar.setMaximum(100);
    }

    return progressBar;
  }

  /**
   * A builder for {@link ProgressDialog}.
   */
  public interface Builder extends DialogBuilder<Builder> {

    /**
     * @param indeterminate the indeterminate status of the progress bar
     * @return this ProgressDialogBuilder instance
     */
    Builder indeterminate(boolean indeterminate);

    /**
     * @param stringPainted the string painted status of the progress bar
     * @return this ProgressDialogBuilder instance
     */
    Builder stringPainted(boolean stringPainted);

    /**
     * @param northPanel if specified this panel is added to the {@link BorderLayout#NORTH} position
     * @return this ProgressDialogBuilder instance
     */
    Builder northPanel(JPanel northPanel);

    /**
     * @param westPanel if specified this panel is added to the {@link BorderLayout#WEST} position
     * @return this ProgressDialogBuilder instance
     */
    Builder westPanel(JPanel westPanel);

    /**
     * @param buttonControls if specified buttons based on these controls are added to the {@link BorderLayout#SOUTH} position
     * @return this ProgressDialogBuilder instance
     */
    Builder buttonControls(Controls buttonControls);

    /**
     * @return a new ProgressDialog
     */
    ProgressDialog build();
  }
}
