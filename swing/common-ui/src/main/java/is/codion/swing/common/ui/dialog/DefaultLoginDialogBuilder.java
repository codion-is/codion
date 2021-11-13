/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.user.User;

import javax.swing.JComponent;

import static java.util.Objects.requireNonNull;

final class DefaultLoginDialogBuilder extends AbstractDialogBuilder<LoginDialogBuilder> implements LoginDialogBuilder {

  private User defaultUser;
  private LoginValidator validator = user -> {};
  private JComponent southComponent;

  @Override
  public LoginDialogBuilder defaultUser(final User defaultUser) {
    this.defaultUser = defaultUser;
    return this;
  }

  @Override
  public LoginDialogBuilder validator(final LoginValidator validator) {
    this.validator = requireNonNull(validator);
    return this;
  }

  @Override
  public LoginDialogBuilder southComponent(final JComponent southComponent) {
    this.southComponent = southComponent;
    return this;
  }

  @Override
  public User show() {
    return new LoginPanel(defaultUser, validator, icon, southComponent).showLoginPanel(owner, title);
  }
}
