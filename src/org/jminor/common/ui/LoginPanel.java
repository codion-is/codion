/*
 * Copyright (c) 2008, Bj�rn Darri Sigur�sson. All Rights Reserved.
 */
package org.jminor.common.ui;

import org.jminor.common.db.User;
import org.jminor.common.i18n.Messages;
import org.jminor.common.model.UserCancelException;
import org.jminor.common.ui.layout.FlexibleGridLayout;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import java.awt.BorderLayout;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

public class LoginPanel extends JPanel {

  public final JTextField txtUsername = new JTextField(8);
  public final JPasswordField txtPassword = new JPasswordField(8);

  private final JLabel lblUser = new JLabel("", JLabel.RIGHT);
  private final JLabel lblPass = new JLabel("", JLabel.RIGHT);

  private final User defaultUser;

  public LoginPanel(final User defaultUser) {
    this(defaultUser, false, null, null);
  }

  public LoginPanel(final User defaultUser, final boolean labelsOnTop,
                    final String userLabel, final String passLabel) {
    this.defaultUser = defaultUser;
    initUI(labelsOnTop, userLabel, passLabel);
  }

  public User getUser() {
    return new User(txtUsername.getText(), new String(txtPassword.getPassword()));
  }

  public static User showLoginPanel(final JComponent parent, final User defaultUser) throws UserCancelException {
    return showLoginPanel(parent, defaultUser, null);
  }

  public static User showLoginPanel(final JComponent parent, final User defaultUser, final Icon icon)
          throws UserCancelException {
    return showLoginPanel(parent, defaultUser, icon, null, null, null);
  }

  public static User showLoginPanel(final JComponent parent, final User defaultUser,
                                    final Icon icon, final String dialogTitle,
                                    final String usernameLabel, final String passwordLabel) throws UserCancelException {
    final LoginPanel panel = new LoginPanel(defaultUser, false, usernameLabel, passwordLabel);

    final JOptionPane pane = new JOptionPane(panel, JOptionPane.PLAIN_MESSAGE,
            JOptionPane.OK_CANCEL_OPTION, icon);

    final JDialog dialog = pane.createDialog(parent, dialogTitle == null ?
            Messages.get(Messages.LOGIN) : dialogTitle);
    dialog.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);
    dialog.pack();
    UiUtil.centerWindow(dialog);
    dialog.setResizable(false);
    dialog.addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent e) {
        panel.init();
      }
      public void windowClosed(WindowEvent e) {
        dialog.dispose();
      }
    });
    dialog.setVisible(true);

    if (pane.getValue() != null && pane.getValue().equals(0))
      return panel.getUser();
    else
      throw new UserCancelException();
  }

  public static User getUser(final JComponent parent, final User defaultUser) throws UserCancelException {
    return showLoginPanel(parent, defaultUser);
  }

  protected void initUI(final boolean labelsOnTop, final String userLabel, final String passLabel) {
    final JPanel retBase = new JPanel(new FlexibleGridLayout(labelsOnTop ? 4 : 2, labelsOnTop ? 1 : 2,5,5,true,false));
    lblUser.setHorizontalAlignment(labelsOnTop ? JLabel.LEADING : JLabel.RIGHT);
    lblPass.setHorizontalAlignment(labelsOnTop ? JLabel.LEADING : JLabel.RIGHT);
    lblUser.setText(userLabel == null ? Messages.get(Messages.USERNAME) : userLabel);
    lblPass.setText(passLabel == null ? Messages.get(Messages.PASSWORD) : passLabel);
    txtUsername.setText(defaultUser == null ? "" : defaultUser.getUsername());
    txtPassword.setText(defaultUser == null ? "" : defaultUser.getPassword());

    txtUsername.setColumns(8);
    txtPassword.setColumns(8);
    txtUsername.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        txtUsername.selectAll();
      }
    });
    txtPassword.addFocusListener(new FocusAdapter() {
      public void focusGained(FocusEvent e) {
        txtPassword.selectAll();
      }
    });

    retBase.add(lblUser);
    retBase.add(txtUsername);

    retBase.add(lblPass);
    retBase.add(txtPassword);

    setLayout(new BorderLayout());
    add(retBase, BorderLayout.CENTER);
    init();
  }

  private void init() {
    if (txtUsername.getText().length() == 0) {
      txtUsername.requestFocusInWindow();
      txtUsername.setCaretPosition(txtUsername.getText().length());
    }
    else {
      txtPassword.requestFocusInWindow();
      txtPassword.setCaretPosition(txtPassword.getPassword().length);
    }
  }
}