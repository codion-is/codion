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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
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
		FrameworkMessages.lookup();
		FrameworkMessages.lookupMnemonic();
		FrameworkMessages.update();
		FrameworkMessages.updateMnemonic();
		FrameworkMessages.updateTip();
		FrameworkMessages.edit();
		FrameworkMessages.editMnemonic();
		FrameworkMessages.editValueTip();
		FrameworkMessages.editSelectedTip();
		FrameworkMessages.delete();
		FrameworkMessages.deleteMnemonic();
		FrameworkMessages.deleteCurrentTip();
		FrameworkMessages.deleteSelectedTip();
		FrameworkMessages.dependencies();
		FrameworkMessages.dependenciesTip();
		FrameworkMessages.insert();
		FrameworkMessages.insertMnemonic();
		FrameworkMessages.insertTip();
		FrameworkMessages.add();
		FrameworkMessages.addMnemonic();
		FrameworkMessages.addTip();
		FrameworkMessages.save();
		FrameworkMessages.saveMnemonic();
		FrameworkMessages.confirmExit();
		FrameworkMessages.confirmExitTitle();
		FrameworkMessages.modifiedWarning();
		FrameworkMessages.modifiedWarningTitle();
		FrameworkMessages.confirmUpdate();
		FrameworkMessages.confirmDelete(42);
		FrameworkMessages.confirmDelete(1);
		FrameworkMessages.confirmInsert();
		FrameworkMessages.noSearchResults();
		FrameworkMessages.searchNoun();
		FrameworkMessages.searchVerb();
		FrameworkMessages.filterNoun();
		FrameworkMessages.filterVerb();
		FrameworkMessages.searchMnemonic();
		FrameworkMessages.copyTableWithHeader();
		FrameworkMessages.settings();
		FrameworkMessages.selectInputField();
	}
}
