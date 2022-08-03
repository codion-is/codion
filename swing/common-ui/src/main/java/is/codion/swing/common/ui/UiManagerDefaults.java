/*
 * Copyright (c) 2015 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    UIManager.put("OptionPane.yesButtonText", Messages.yes());
    UIManager.put("OptionPane.noButtonText", Messages.no());
    UIManager.put("OptionPane.cancelButtonText", Messages.cancel());
    UIManager.put("OptionPane.okButtonText", Messages.ok());
    UIManager.put("OptionPane.inputDialogTitle", SwingMessages.get("OptionPane.inputDialogTitle"));
    UIManager.put("OptionPane.messageDialogTitle", SwingMessages.get("OptionPane.messageDialogTitle"));

    UIManager.put("FileChooser.newFolderErrorText", SwingMessages.get("FileChooser.newFolderErrorText"));
    UIManager.put("FileChooser.newFolderErrorSeparator", SwingMessages.get("FileChooser.newFolderErrorSeparator"));

    UIManager.put("FileChooser.newFolderParentDoesntExistTitleText", SwingMessages.get("FileChooser.newFolderParentDoesntExistTitleText"));
    UIManager.put("FileChooser.newFolderParentDoesntExistText", SwingMessages.get("FileChooser.newFolderParentDoesntExistText"));

    UIManager.put("FileChooser.fileDescriptionText", SwingMessages.get("FileChooser.fileDescriptionText"));
    UIManager.put("FileChooser.directoryDescriptionText", SwingMessages.get("FileChooser.directoryDescriptionText"));

    UIManager.put("FileChooser.saveButtonText", SwingMessages.get("FileChooser.saveButtonText"));
    UIManager.put("FileChooser.openButtonText", SwingMessages.get("FileChooser.openButtonText"));
    UIManager.put("FileChooser.saveDialogTitleText", SwingMessages.get("FileChooser.saveDialogTitleText"));
    UIManager.put("FileChooser.openDialogTitleText", SwingMessages.get("FileChooser.openDialogTitleText"));
    UIManager.put("FileChooser.cancelButtonText", SwingMessages.get("FileChooser.cancelButtonText"));
    UIManager.put("FileChooser.updateButtonText", SwingMessages.get("FileChooser.updateButtonText"));
    UIManager.put("FileChooser.helpButtonText", SwingMessages.get("FileChooser.helpButtonText"));
    UIManager.put("FileChooser.directoryOpenButtonText", SwingMessages.get("FileChooser.directoryOpenButtonText"));

    UIManager.put("FileChooser.saveButtonMnemonic", SwingMessages.get("FileChooser.saveButtonMnemonic").charAt(0));
    UIManager.put("FileChooser.openButtonMnemonic", SwingMessages.get("FileChooser.openButtonMnemonic").charAt(0));
    UIManager.put("FileChooser.cancelButtonMnemonic", SwingMessages.get("FileChooser.cancelButtonMnemonic").charAt(0));
    UIManager.put("FileChooser.updateButtonMnemonic", SwingMessages.get("FileChooser.updateButtonMnemonic").charAt(0));
    UIManager.put("FileChooser.helpButtonMnemonic", SwingMessages.get("FileChooser.helpButtonMnemonic").charAt(0));
    UIManager.put("FileChooser.directoryOpenButtonMnemonic", SwingMessages.get("FileChooser.directoryOpenButtonMnemonic").charAt(0));

    UIManager.put("FileChooser.saveButtonToolTipText", SwingMessages.get("FileChooser.saveButtonToolTipText"));
    UIManager.put("FileChooser.openButtonToolTipText", SwingMessages.get("FileChooser.openButtonToolTipText"));
    UIManager.put("FileChooser.cancelButtonToolTipText", SwingMessages.get("FileChooser.cancelButtonToolTipText"));
    UIManager.put("FileChooser.updateButtonToolTipText", SwingMessages.get("FileChooser.updateButtonToolTipText"));
    UIManager.put("FileChooser.helpButtonToolTipText", SwingMessages.get("FileChooser.helpButtonToolTipText"));
    UIManager.put("FileChooser.directoryOpenButtonToolTipText", SwingMessages.get("FileChooser.directoryOpenButtonToolTipText"));

    UIManager.put("FileChooser.acceptAllFileFilterText", SwingMessages.get("FileChooser.acceptAllFileFilterText"));
    UIManager.put("FileChooser.byDateText", SwingMessages.get("FileChooser.byDateText"));
    UIManager.put("FileChooser.byNameText", SwingMessages.get("FileChooser.byNameText"));
    UIManager.put("FileChooser.chooseButtonText", SwingMessages.get("FileChooser.chooseButtonText"));
    UIManager.put("FileChooser.createButtonText", SwingMessages.get("FileChooser.createButtonText"));
    UIManager.put("FileChooser.desktopName", SwingMessages.get("FileChooser.desktopName"));
    UIManager.put("FileChooser.fileNameLabelMnemonic", SwingMessages.get("FileChooser.fileNameLabelMnemonic"));
    UIManager.put("FileChooser.fileNameLabelText", SwingMessages.get("FileChooser.fileNameLabelText"));
    UIManager.put("FileChooser.filesOfTypeLabelMnemonic", SwingMessages.get("FileChooser.filesOfTypeLabelMnemonic"));
    UIManager.put("FileChooser.filesOfTypeLabelText", SwingMessages.get("FileChooser.filesOfTypeLabelText"));
    UIManager.put("FileChooser.lookInLabelText", SwingMessages.get("FileChooser.lookInLabelText"));
    UIManager.put("FileChooser.lookInLabelMnemonic", SwingMessages.get("FileChooser.lookInLabelMnemonic"));
    UIManager.put("FileChooser.newFolderButtonText", SwingMessages.get("FileChooser.newFolderButtonText"));
    UIManager.put("FileChooser.newFolderExistsErrorText", SwingMessages.get("FileChooser.newFolderExistsErrorText"));
    UIManager.put("FileChooser.newFolderPromptText", SwingMessages.get("FileChooser.newFolderPromptText"));
    UIManager.put("FileChooser.newFolderTitleText", SwingMessages.get("FileChooser.newFolderTitleText"));
    UIManager.put("FileChooser.saveTitleText", SwingMessages.get("FileChooser.saveTitleText"));
    UIManager.put("FileChooser.openTitleText", SwingMessages.get("FileChooser.openTitleText"));
    UIManager.put("FileChooser.upFolderToolTipText", SwingMessages.get("FileChooser.upFolderToolTipText"));
    UIManager.put("FileChooser.homeFolderToolTipText", SwingMessages.get("FileChooser.homeFolderToolTipText"));
    UIManager.put("FileChooser.newFolderToolTipText", SwingMessages.get("FileChooser.newFolderToolTipText"));
    UIManager.put("FileChooser.listViewButtonToolTipText", SwingMessages.get("FileChooser.listViewButtonToolTipText"));
    UIManager.put("FileChooser.detailsViewButtonToolTipText", SwingMessages.get("FileChooser.detailsViewButtonToolTipText"));

    //otherwise, a hierarchy of tabbed panes looks crappy
    UIManager.put("TabbedPane.contentBorderInsets", new Insets(2, 0, 0, 0));
  }

  private UiManagerDefaults() {}

  /**
   * A convenience method for triggering a loading of this class, which sets the UIManager defaults
   */
  public static void initialize() {/*Just for loading the class*/}
}
