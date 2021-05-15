/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.user.User;
import is.codion.swing.common.ui.Windows;
import is.codion.swing.common.ui.dialog.Dialogs.LoginDialogBuilder;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import java.awt.Window;

import static java.util.Objects.requireNonNull;

final class DefaultLoginDialogBuilder implements LoginDialogBuilder {

  private Window owner;
  private User defaultUser;
  private Dialogs.LoginValidator validator = user -> {};
  private JComponent southComponent;
  private String title;
  private ImageIcon icon;

  @Override
  public LoginDialogBuilder owner(final Window owner) {
    this.owner = owner;
    return this;
  }

  @Override
  public LoginDialogBuilder dialogParent(final JComponent dialogParent) {
    if (owner != null) {
      throw new IllegalStateException("owner has alrady been set");
    }
    this.owner = dialogParent == null ? null : Windows.getParentWindow(dialogParent);
    return this;
  }

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
  public LoginDialogBuilder title(final String title) {
    this.title = title;
    return this;
  }

  @Override
  public LoginDialogBuilder icon(final ImageIcon icon) {
    this.icon = icon;
    return this;
  }

  @Override
  public User show() {
    return new LoginPanel(defaultUser, validator, southComponent).showLoginPanel(owner, title, icon);
  }
}
