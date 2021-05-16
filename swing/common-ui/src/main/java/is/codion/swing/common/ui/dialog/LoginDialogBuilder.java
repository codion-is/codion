/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Window;

/**
 * A login dialog builder.
 */
public interface LoginDialogBuilder {

  /**
   * @param owner the dialog owner
   * @return this LoginDialogBuilder instance
   */
  LoginDialogBuilder owner(Window owner);

  /**
   * @param dialogParent the dialog parent component
   * @return this LoginDialogBuilder instance
   */
  LoginDialogBuilder dialogParent(JComponent dialogParent);

  /**
   * @param defaultUser the default user credentials to display
   * @return this LoginDialogBuilder instance
   */
  LoginDialogBuilder defaultUser(User defaultUser);

  /**
   * @param validator the login validator to use
   * @return this LoginDialogBuilder instance
   */
  LoginDialogBuilder validator(LoginValidator validator);

  /**
   * @param southComponent a component to add to the south of the credentials input fields
   * @return this LoginDialogBuilder instance
   */
  LoginDialogBuilder southComponent(JComponent southComponent);

  /**
   * @param title the dialog title
   * @return this LoginDialogBuilder instance
   */
  LoginDialogBuilder title(String title);

  /**
   * @param icon the dialog icon
   * @return this LoginDialogBuilder instance
   */
  LoginDialogBuilder icon(ImageIcon icon);

  /**
   * @return the logged in user
   * @throws CancelException in case the login is cancelled
   */
  User show();

  /**
   * Validates a login attempt.
   */
  interface LoginValidator {

    /**
     * Valdates a login with the given user
     * @param user the user
     * @throws Exception in case validation fails
     */
    void validate(User user) throws Exception;
  }
}
