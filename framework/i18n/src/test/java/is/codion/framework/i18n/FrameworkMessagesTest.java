/*
 * Copyright (c) 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.i18n;

import org.junit.jupiter.api.Test;

public class FrameworkMessagesTest {

  @Test
  void messages() {
    FrameworkMessages.file();
    FrameworkMessages.fileMnemonic();
    FrameworkMessages.exit();
    FrameworkMessages.exitMnemonic();
    FrameworkMessages.exitTip();
    FrameworkMessages.view();
    FrameworkMessages.viewMnemonic();
    FrameworkMessages.refreshAll();
    FrameworkMessages.supportTables();
    FrameworkMessages.supportTablesMnemonic();
    FrameworkMessages.update();
    FrameworkMessages.updateMnemonic();
    FrameworkMessages.updateTip();
    FrameworkMessages.edit();
    FrameworkMessages.editSelectedTip();
    FrameworkMessages.delete();
    FrameworkMessages.deleteMnemonic();
    FrameworkMessages.deleteCurrentTip();
    FrameworkMessages.deleteSelectedTip();
    FrameworkMessages.refresh();
    FrameworkMessages.refreshMnemonic();
    FrameworkMessages.refreshTip();
    FrameworkMessages.dependencies();
    FrameworkMessages.dependenciesTip();
    FrameworkMessages.add();
    FrameworkMessages.addMnemonic();
    FrameworkMessages.addTip();
    FrameworkMessages.save();
    FrameworkMessages.saveMnemonic();
    FrameworkMessages.confirmExit();
    FrameworkMessages.confirmExitTitle();
    FrameworkMessages.unsavedDataWarning();
    FrameworkMessages.unsavedDataWarningTitle();
    FrameworkMessages.confirmUpdate();
    FrameworkMessages.confirmDeleteSelected();
    FrameworkMessages.confirmDelete();
    FrameworkMessages.confirmInsert();
    FrameworkMessages.show();
    FrameworkMessages.noResultsFound();
    FrameworkMessages.search();
    FrameworkMessages.filter();
    FrameworkMessages.searchMnemonic();
    FrameworkMessages.copyCell();
    FrameworkMessages.copyTableWithHeader();
    FrameworkMessages.settings();
    FrameworkMessages.selectInputField();
    FrameworkMessages.selectSearchField();
    FrameworkMessages.selectFilterField();
  }
}
