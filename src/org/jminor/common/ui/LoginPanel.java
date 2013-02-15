/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.User;

import javax.swing.AbstractAction;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;

/**
 * A JPanel for retrieving login information.
 */
public final class LoginPanel extends JPanel {

  private static final int DEFAULT_FIELD_COLUMNS = 8;

  private final JTextField usernameField = new JTextField(8);
  private final JPasswordField passwordField = new JPasswordField(8);

  private final JLabel lblUser = new JLabel("", JLabel.RIGHT);
  private final JLabel lblPass = new JLabel("", JLabel.RIGHT);

  private final User defaultUser;

  public LoginPanel(final User defaultUser) {
    this(defaultUser, false, null, null);
  }

  public LoginPanel(final User defaultUser, final boolean labelsOnTop, final String userLabel, final String passLabel) {
    this.defaultUser = defaultUser;
    initUI(labelsOnTop, userLabel, passLabel);
  }

  public User getUser() {
    return new User(usernameField.getText(), new String(passwordField.getPassword()));
  }

  public static User showLoginPanel(final JComponent parent, final User defaultUser) throws CancelException {
    return showLoginPanel(parent, defaultUser, null);
  }

  public static User showLoginPanel(final JComponent parent, final User defaultUser, final Icon icon)
          throws CancelException {
    return showLoginPanel(parent, defaultUser, icon, null, null, null);
  }

  public static User showLoginPanel(final JComponent parent, final User defaultUser,
                                    final Icon icon, final String dialogTitle,
                                    final String usernameLabel, final String passwordLabel) throws CancelException {
    final LoginPanel panel = new LoginPanel(defaultUser, false, usernameLabel, passwordLabel);
    final JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE, JOptionPane.OK_CANCEL_OPTION, icon);
    final JDialog dialog = pane.createDialog(parent, dialogTitle == null ? Messages.get(Messages.LOGIN) : dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.pack();
    UiUtil.centerWindow(dialog);
    dialog.setResizable(false);
    dialog.setVisible(true);

    if (pane.getValue() != null && pane.getValue().equals(0)) {
      return panel.getUser();
    }
    else {
      throw new CancelException();
    }
  }

  public JPasswordField getPasswordField() {
    return passwordField;
  }

  public JTextField getUsernameField() {
    return usernameField;
  }

  public static User getUser(final JComponent parent, final User defaultUser) throws CancelException {
    return showLoginPanel(parent, defaultUser);
  }

  private void initUI(final boolean labelsOnTop, final String userLabel, final String passLabel) {
    final JPanel retBase = new JPanel(UiUtil.createFlexibleGridLayout(labelsOnTop ? 4 : 2, labelsOnTop ? 1 : 2, true, false));
    lblUser.setHorizontalAlignment(labelsOnTop ? JLabel.LEADING : JLabel.RIGHT);
    lblPass.setHorizontalAlignment(labelsOnTop ? JLabel.LEADING : JLabel.RIGHT);
    lblUser.setText(userLabel == null ? Messages.get(Messages.USERNAME) : userLabel);
    lblPass.setText(passLabel == null ? Messages.get(Messages.PASSWORD) : passLabel);
    usernameField.setText(defaultUser == null ? "" : defaultUser.getUsername());
    passwordField.setText(defaultUser == null ? "" : defaultUser.getPassword());

    usernameField.setColumns(DEFAULT_FIELD_COLUMNS);
    passwordField.setColumns(DEFAULT_FIELD_COLUMNS);
    UiUtil.selectAllOnFocusGained(usernameField);
    UiUtil.selectAllOnFocusGained(passwordField);

    retBase.add(lblUser);
    retBase.add(usernameField);

    retBase.add(lblPass);
    retBase.add(passwordField);

    setLayout(new FlowLayout(FlowLayout.CENTER));
    add(retBase, BorderLayout.CENTER);
    if (usernameField.getText().length() == 0) {
      UiUtil.addInitialFocusHack(usernameField, new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          usernameField.setCaretPosition(usernameField.getText().length());
        }
      });
    }
    else {
      UiUtil.addInitialFocusHack(passwordField, new AbstractAction() {
        @Override
        public void actionPerformed(final ActionEvent e) {
          passwordField.setCaretPosition(passwordField.getPassword().length);
        }
      });
    }
  }
}