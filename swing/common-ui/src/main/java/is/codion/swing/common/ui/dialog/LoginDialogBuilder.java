/*
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.model.CancelException;
import is.codion.common.user.User;

import javax.swing.JComponent;

/**
 * A login dialog builder.
 */
public interface LoginDialogBuilder extends DialogBuilder<LoginDialogBuilder> {

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
   * @param inputFieldColumns the number of columns to display in the input fields (username/password), 8 by default
   * @return this LoginDialogBuilder instance
   */
  LoginDialogBuilder inputFieldColumns(int inputFieldColumns);

  /**
   * @return the logged-in user
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
