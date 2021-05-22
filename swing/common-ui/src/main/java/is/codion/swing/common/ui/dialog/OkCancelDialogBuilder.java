/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import java.awt.Window;


/**
 * Builds a modal dialog for displaying the given {@code component},
 * with OK and Cancel buttons based on the given actions.
 * An OK action must be provided and the default Cancel action simply disposes the dialog.
 */
public interface OkCancelDialogBuilder {

  /**
   * @param owner the dialog owner
   * @return this builder instance
   */
  OkCancelDialogBuilder owner(Window owner);

  /**
   * Sets the dialog owner as the parent window of the given component.
   * @param owner the dialog parent component
   * @return this builder instance
   */
  OkCancelDialogBuilder owner(JComponent owner);

  /**
   * @param component the component to display
   * @return this builder instance
   */
  OkCancelDialogBuilder component(JComponent component);

  /**
   * @param title the dialog title
   * @return this builder instance
   */
  OkCancelDialogBuilder title(String title);

  /**
   * @param icon the dialog icon
   * @return this builder instance
   */
  OkCancelDialogBuilder icon(ImageIcon icon);

  /**
   * @param okAction the action for the OK button
   * @return this builder instance
   */
  OkCancelDialogBuilder okAction(Action okAction);

  /**
   * @param cancelAction the action for the Cancel button
   * @return this builder instance
   */
  OkCancelDialogBuilder cancelAction(Action cancelAction);

  /**
   * Builds and shows the dialog.
   * @return a new JDialog instance based on this builder.
   * @throws IllegalStateException in case no component has been specified
   */
  JDialog show();

  /**
   * @return a new JDialog instance based on this builder.
   * @throws IllegalStateException in case no component has been specified
   */
  JDialog build();
}
