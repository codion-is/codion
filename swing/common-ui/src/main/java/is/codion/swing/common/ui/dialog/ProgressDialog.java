/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.Sizes;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.WindowConstants;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Window;

/**
 * A dialog containing a progress bar.
 * @see Dialogs#progressDialog()
 */
public final class ProgressDialog extends JDialog {

  public static final int DEFAULT_PROGRESS_BAR_WIDTH = 400;

  private final JProgressBar progressBar;

  ProgressDialog(final DefaultProgressDialogBuilder builder, final Window dialogOwner) {
    super(dialogOwner, dialogOwner == null ? ModalityType.MODELESS : ModalityType.APPLICATION_MODAL);
    setTitle(builder.title);
    if (builder.icon != null) {
      setIconImage(builder.icon.getImage());
    }
    setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
    progressBar = initializeProgressBar(builder.indeterminate, builder.stringPainted, builder.progressBarSize);
    initializeUI(builder.northPanel, builder.westPanel, builder.controls);
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
   * @param controls if specified buttons based on these controls are added to this dialog
   */
  private void initializeUI(final JPanel northPanel, final JPanel westPanel, final Controls controls) {
    setLayout(Layouts.borderLayout());
    if (northPanel != null) {
      add(northPanel, BorderLayout.NORTH);
    }
    if (westPanel != null) {
      add(westPanel, BorderLayout.WEST);
    }
    add(progressBar, BorderLayout.CENTER);
    if (controls != null) {
      final JPanel southPanel = new JPanel(Layouts.flowLayout(FlowLayout.TRAILING));
      southPanel.add(controls.createHorizontalButtonPanel());
      add(southPanel, BorderLayout.SOUTH);
    }
    pack();
  }

  private static JProgressBar initializeProgressBar(final boolean indeterminate, final boolean stringPainted,
                                                    final Dimension size) {
    final JProgressBar progressBar = new JProgressBar();
    progressBar.setStringPainted(stringPainted);
    if (size != null) {
      progressBar.setPreferredSize(size);
    }
    else {
      Sizes.setPreferredWidth(progressBar, DEFAULT_PROGRESS_BAR_WIDTH);
    }
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
     * @param controls if specified buttons based on these controls are added to the {@link BorderLayout#SOUTH} position
     * @return this ProgressDialogBuilder instance
     */
    Builder controls(Controls controls);

    /**
     * @param progressBarSize the progress bar size
     * @return this ProgressDialogBuilder instance
     */
    Builder progressBarSize(Dimension progressBarSize);

    /**
     * @return a new ProgressDialog
     */
    ProgressDialog build();
  }

  static class DefaultProgressDialogBuilder extends AbstractDialogBuilder<Builder> implements Builder {

    private boolean indeterminate = true;
    private boolean stringPainted = false;
    private JPanel northPanel;
    private JPanel westPanel;
    private Controls controls;
    private Dimension progressBarSize;

    @Override
    public Builder indeterminate(final boolean indeterminate) {
      this.indeterminate = indeterminate;
      return this;
    }

    @Override
    public Builder stringPainted(final boolean stringPainted) {
      this.stringPainted = stringPainted;
      return this;
    }

    @Override
    public Builder northPanel(final JPanel northPanel) {
      this.northPanel = northPanel;
      return this;
    }

    @Override
    public Builder westPanel(final JPanel westPanel) {
      this.westPanel = westPanel;
      return this;
    }

    @Override
    public Builder controls(final Controls controls) {
      this.controls = controls;
      return this;
    }

    @Override
    public Builder progressBarSize(final Dimension progressBarSize) {
      this.progressBarSize = progressBarSize;
      return this;
    }

    @Override
    public ProgressDialog build() {
      return new ProgressDialog(this, owner);
    }
  }
}
