/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui;

import org.jminor.common.i18n.Messages;

import javax.swing.UIManager;
import java.awt.Insets;

public final class UiManagerDefaults {

  static {
    UIManager.put("OptionPane.yesButtonText", Messages.get(Messages.YES));
    UIManager.put("OptionPane.noButtonText", Messages.get(Messages.NO));
    UIManager.put("OptionPane.cancelButtonText", Messages.get(Messages.CANCEL));
    UIManager.put("OptionPane.okButtonText", Messages.get(Messages.OK));
    UIManager.put("OptionPane.inputDialogTitle", SwingMessages.get("OptionPane.inputDialogTitle"));
    UIManager.put("OptionPane.messageDialogTitle", SwingMessages.get("OptionPane.messageDialogTitle"));
    //otherwise a hierarchy of tabbed panes looks crappy
    UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 0));
  }

  private UiManagerDefaults() {}

  /**
   * A convenience method for triggering a loading of this class, which sets the UIManager defaults
   */
  public static void init() {/*Just for loading the class*/}
}
