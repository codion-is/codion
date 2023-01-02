/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.common.value.Value;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;

import static java.util.Objects.requireNonNull;

final class DefaultLoginDialogBuilder extends AbstractDialogBuilder<LoginDialogBuilder> implements LoginDialogBuilder {

  private static final int DEFAULT_FIELD_COLUMNS = 8;

  private User defaultUser;
  private LoginValidator validator = new NoLoginValidation();
  private JComponent southComponent;
  private int inputFieldColumns = DEFAULT_FIELD_COLUMNS;

  DefaultLoginDialogBuilder() {
    titleProvider(Value.value(Messages.login()));
  }

  @Override
  public LoginDialogBuilder defaultUser(User defaultUser) {
    this.defaultUser = defaultUser;
    return this;
  }

  @Override
  public LoginDialogBuilder validator(LoginValidator validator) {
    this.validator = requireNonNull(validator);
    return this;
  }

  @Override
  public LoginDialogBuilder southComponent(JComponent southComponent) {
    this.southComponent = southComponent;
    return this;
  }

  @Override
  public LoginDialogBuilder inputFieldColumns(int inputFieldColumns) {
    this.inputFieldColumns = inputFieldColumns;
    return this;
  }

  @Override
  public User show() {
    JFrame dummyFrame = null;
    if (owner == null && isWindows()) {
      owner = dummyFrame = createDummyFrame(titleProvider == null ? null : titleProvider.get(), icon);
    }
    LoginPanel loginPanel = new LoginPanel(defaultUser, validator, icon, southComponent, inputFieldColumns);
    new DefaultOkCancelDialogBuilder(loginPanel)
            .owner(owner)
            .resizable(false)
            .titleProvider(titleProvider)
            .icon(icon)
            .okAction(loginPanel.okControl())
            .cancelAction(loginPanel.cancelControl())
            .show();
    if (dummyFrame != null) {
      dummyFrame.dispose();
    }

    User user = loginPanel.user();
    if (user == null) {
      throw new CancelException();
    }

    return user;
  }

  private static JFrame createDummyFrame(String title, ImageIcon icon) {
    JFrame frame = new JFrame(title);
    frame.setUndecorated(true);
    frame.setVisible(true);
    frame.setLocationRelativeTo(null);
    if (icon != null) {
      frame.setIconImage(icon.getImage());
    }

    return frame;
  }

  private static boolean isWindows() {
    return System.getProperty("os.name").toLowerCase().contains("win");
  }

  private static final class NoLoginValidation implements LoginValidator {

    @Override
    public void validate(User user) throws Exception {}
  }
}
