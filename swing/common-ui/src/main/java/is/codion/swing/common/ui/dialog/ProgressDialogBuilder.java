/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.control.Controls;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;
import java.awt.Window;

/**
 * A builder for {@link ProgressDialog}.
 */
public interface ProgressDialogBuilder {

  /**
   * @param owner the dialog owner
   * @return this ProgressDialogBuilder instance
   */
  ProgressDialogBuilder owner(Window owner);

  /**
   * @param dialogParent the dialog parent component
   * @return this ProgressDialogBuilder instance
   */
  ProgressDialogBuilder dialogParent(JComponent dialogParent);

  /**
   * @param title the title
   * @return this ProgressDialogBuilder instance
   */
  ProgressDialogBuilder title(String title);

  /**
   * @param indeterminate the indeterminate status of the progress bar
   * @return this ProgressDialogBuilder instance
   */
  ProgressDialogBuilder indeterminate(boolean indeterminate);

  /**
   * @param stringPainted the string painted status of the progress bar
   * @return this ProgressDialogBuilder instance
   */
  ProgressDialogBuilder stringPainted(boolean stringPainted);

  /**
   * @param northPanel if specified this panel is added to the {@link BorderLayout#NORTH} position
   * @return this ProgressDialogBuilder instance
   */
  ProgressDialogBuilder northPanel(JPanel northPanel);

  /**
   * @param westPanel if specified this panel is added to the {@link BorderLayout#WEST} position
   * @return this ProgressDialogBuilder instance
   */
  ProgressDialogBuilder westPanel(JPanel westPanel);

  /**
   * @param buttonControls if specified buttons based on these controls are added to the {@link BorderLayout#SOUTH}
   * position
   * @return this ProgressDialogBuilder instance
   */
  ProgressDialogBuilder buttonControls(Controls buttonControls);

  /**
   * @return a new ProgressDialog
   */
  ProgressDialog build();
}
