/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.UIManager;
import java.util.Locale;
import java.util.ResourceBundle;

public final class SwingMessages {

  static {
    UIManager.put("OptionPane.yesButtonText", Messages.get(Messages.YES));
    UIManager.put("OptionPane.noButtonText", Messages.get(Messages.NO));
    UIManager.put("OptionPane.cancelButtonText", Messages.get(Messages.CANCEL));
    UIManager.put("OptionPane.okButtonText", Messages.get(Messages.OK));
    UIManager.put("OptionPane.inputDialogTitle", FrameworkMessages.get(FrameworkMessages.OPTION_PANE_INPUT_DIALOG_TITLE));
    UIManager.put("OptionPane.messageDialogTitle", FrameworkMessages.get(FrameworkMessages.OPTION_PANE_MESSAGE_DIALOG_TITLE));
  }

  public static void init() {}
}
