/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Window;

/**
 * An exception dialog builder.
 */
public interface ExceptionDialogBuilder {

  /**
   * @param owner the dialog owner
   * @return this ExceptionDialogBuilder instance
   */
  ExceptionDialogBuilder owner(Window owner);

  /**
   * @param dialogParent the dialog parent component
   * @return this ExceptionDialogBuilder instance
   */
  ExceptionDialogBuilder dialogParent(JComponent dialogParent);

  /**
   * @param title the dialog title
   * @return this ExceptionDialogBuilder instance
   */
  ExceptionDialogBuilder title(String title);

  /**
   * @param icon the dialog icon
   * @return this ExceptionDialogBuilder instance
   */
  ExceptionDialogBuilder icon(ImageIcon icon);

  /**
   * @param message the message to display
   * @return this ExceptionDialogBuilder instance
   */
  ExceptionDialogBuilder message(String message);

  /**
   * Displays an exception dialog for the given exception
   * @param exception the exception to display
   */
  void show(Throwable exception);
}
