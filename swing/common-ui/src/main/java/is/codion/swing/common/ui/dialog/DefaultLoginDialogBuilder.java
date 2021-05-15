/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.user.User;

import javax.swing.ImageIcon;
import javax.swing.JComponent;

import static java.util.Objects.requireNonNull;

public final class DefaultLoginDialogBuilder implements Dialogs.LoginDialogBuilder {

  private User defaultUser;
  private Dialogs.LoginValidator validator = user -> {};
  private JComponent southComponent;
  private JComponent dialogParent;
  private String dialogTitle;
  private ImageIcon icon;

  @Override
  public Dialogs.LoginDialogBuilder defaultUser(final User defaultUser) {
    this.defaultUser = defaultUser;
    return this;
  }

  @Override
  public Dialogs.LoginDialogBuilder validator(final Dialogs.LoginValidator validator) {
    this.validator = requireNonNull(validator);
    return this;
  }

  @Override
  public Dialogs.LoginDialogBuilder southComponent(final JComponent southComponent) {
    this.southComponent = southComponent;
    return this;
  }

  @Override
  public Dialogs.LoginDialogBuilder dialogParent(final JComponent dialogParent) {
    this.dialogParent = dialogParent;
    return this;
  }

  @Override
  public Dialogs.LoginDialogBuilder dialogTitle(final String dialogTitle) {
    this.dialogTitle = dialogTitle;
    return this;
  }

  @Override
  public Dialogs.LoginDialogBuilder icon(final ImageIcon icon) {
    this.icon = icon;
    return this;
  }

  @Override
  public User show() {
    return new LoginPanel(defaultUser, validator, southComponent).showLoginPanel(dialogParent, dialogTitle, icon);
  }
}
