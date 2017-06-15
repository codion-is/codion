/*
 * Copyright (c) 2004 - 2017, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.i18n.Messages;
import org.jminor.framework.i18n.FrameworkMessages;

import javax.swing.UIManager;

/**
 * Handles i18n for Swing components
 */
public final class SwingMessages {

  static {
    UIManager.put("OptionPane.yesButtonText", Messages.get(Messages.YES));
    UIManager.put("OptionPane.noButtonText", Messages.get(Messages.NO));
    UIManager.put("OptionPane.cancelButtonText", Messages.get(Messages.CANCEL));
    UIManager.put("OptionPane.okButtonText", Messages.get(Messages.OK));
    UIManager.put("OptionPane.inputDialogTitle", FrameworkMessages.get(FrameworkMessages.OPTION_PANE_INPUT_DIALOG_TITLE));
    UIManager.put("OptionPane.messageDialogTitle", FrameworkMessages.get(FrameworkMessages.OPTION_PANE_MESSAGE_DIALOG_TITLE));
  }

  private SwingMessages() {}

  /**
   * A convenience method for triggering a loading of this class
   */
  public static void init() {/*Just for loading the class*/}
}
