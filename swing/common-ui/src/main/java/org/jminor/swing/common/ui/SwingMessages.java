/*
 * Copyright (c) 2004 - 2018, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.i18n.Messages;

import javax.swing.UIManager;
import java.util.Locale;
import java.util.ResourceBundle;

/**
 * Handles i18n for Swing components
 */
public final class SwingMessages {

  private static final ResourceBundle MESSAGES = ResourceBundle.getBundle(SwingMessages.class.getName(), Locale.getDefault());

  static {
    UIManager.put("OptionPane.yesButtonText", Messages.get(Messages.YES));
    UIManager.put("OptionPane.noButtonText", Messages.get(Messages.NO));
    UIManager.put("OptionPane.cancelButtonText", Messages.get(Messages.CANCEL));
    UIManager.put("OptionPane.okButtonText", Messages.get(Messages.OK));
    UIManager.put("OptionPane.inputDialogTitle", MESSAGES.getString("OptionPane.inputDialogTitle"));
    UIManager.put("OptionPane.messageDialogTitle", MESSAGES.getString("OptionPane.messageDialogTitle"));
  }

  private SwingMessages() {}

  /**
   * A convenience method for triggering a loading of this class
   */
  public static void init() {/*Just for loading the class*/}
}
