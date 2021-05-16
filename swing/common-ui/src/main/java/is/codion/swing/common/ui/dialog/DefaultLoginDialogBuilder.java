/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.user.User;
import is.codion.swing.common.ui.dialog.Dialogs.LoginDialogBuilder;

import javax.swing.JComponent;

import static java.util.Objects.requireNonNull;

final class DefaultLoginDialogBuilder extends AbstractDialogBuilder<LoginDialogBuilder> implements LoginDialogBuilder {

  private User defaultUser;
  private Dialogs.LoginValidator validator = user -> {};
  private JComponent southComponent;

  @Override
  public LoginDialogBuilder defaultUser(final User defaultUser) {
    this.defaultUser = defaultUser;
    return this;
  }

  @Override
  public LoginDialogBuilder validator(final Dialogs.LoginValidator validator) {
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
    return new LoginPanel(defaultUser, validator, southComponent).showLoginPanel(owner, title, icon);
  }
}
