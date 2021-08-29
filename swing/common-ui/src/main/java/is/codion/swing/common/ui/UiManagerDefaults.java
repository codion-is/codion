/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui;

import is.codion.common.i18n.Messages;

import javax.swing.UIManager;
import java.awt.Insets;

/**
 * A utility class for initializing default UIManager values, default ok, cancel button captions and such.
 */
public final class UiManagerDefaults {

  static {
    UIManager.put("OptionPane.yesButtonText", Messages.get(Messages.YES));
    UIManager.put("OptionPane.noButtonText", Messages.get(Messages.NO));
    UIManager.put("OptionPane.cancelButtonText", Messages.get(Messages.CANCEL));
    UIManager.put("OptionPane.okButtonText", Messages.get(Messages.OK));
    UIManager.put("OptionPane.inputDialogTitle", SwingMessages.get("OptionPane.inputDialogTitle"));
    UIManager.put("OptionPane.messageDialogTitle", SwingMessages.get("OptionPane.messageDialogTitle"));
    //otherwise, a hierarchy of tabbed panes looks crappy
    UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 0));
  }

  private UiManagerDefaults() {}

  /**
   * A convenience method for triggering a loading of this class, which sets the UIManager defaults
   */
  public static void initialize() {/*Just for loading the class*/}
}
