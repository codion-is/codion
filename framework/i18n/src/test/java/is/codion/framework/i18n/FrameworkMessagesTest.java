/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2023, Björn Darri Sigurðsson.
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
    FrameworkMessages.confirmDeleteSelected(42);
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
