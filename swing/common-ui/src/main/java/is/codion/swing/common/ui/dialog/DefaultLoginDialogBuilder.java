/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import is.codion.common.i18n.Messages;
import is.codion.common.model.CancelException;
import is.codion.common.user.User;

import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JFrame;

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
    JFrame dummyFrame = null;
    if (owner == null && isWindows()) {
      owner = dummyFrame = createDummyFrame(title, icon);
    }
    LoginPanel loginPanel = new LoginPanel(defaultUser, validator, icon, southComponent);
    new DefaultOkCancelDialogBuilder(loginPanel)
            .owner(owner)
            .resizable(false)
            .title(title == null ? Messages.get(Messages.LOGIN) : title)
            .icon(icon)
            .okAction(loginPanel.getOkControl())
            .cancelAction(loginPanel.getCancelControl())
            .show();
    if (dummyFrame != null) {
      dummyFrame.dispose();
    }

    User user = loginPanel.getUser();
    if (user == null) {
      throw new CancelException();
    }

    return user;
  }

  private static JFrame createDummyFrame(final String title, final ImageIcon icon) {
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
}
