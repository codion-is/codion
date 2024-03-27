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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.common.i18n;

import org.junit.jupiter.api.Test;

public class MessagesTest {

	@Test
	void messages() {
		Messages.cancel();
		Messages.cancelMnemonic();
		Messages.print();
		Messages.printMnemonic();
		Messages.error();
		Messages.yes();
		Messages.no();
		Messages.ok();
		Messages.okMnemonic();
		Messages.copy();
		Messages.login();
		Messages.username();
		Messages.password();
		Messages.search();
		Messages.clear();
		Messages.clearTip();
		Messages.clearMnemonic();
		Messages.advanced();
		Messages.find();
		Messages.refresh();
		Messages.refreshMnemonic();
		Messages.refreshTip();
	}
}
