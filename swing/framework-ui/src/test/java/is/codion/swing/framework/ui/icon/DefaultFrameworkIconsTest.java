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
 * Copyright (c) 2021 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.icon;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

public final class DefaultFrameworkIconsTest {

	@Test
	void test() {
		DefaultFrameworkIcons icons = new DefaultFrameworkIcons();
		assertNotNull(icons.filter());
		assertNotNull(icons.search());
		assertNotNull(icons.add());
		assertNotNull(icons.delete());
		assertNotNull(icons.update());
		assertNotNull(icons.copy());
		assertNotNull(icons.refresh());
		assertNotNull(icons.clear());
		assertNotNull(icons.up());
		assertNotNull(icons.down());
		assertNotNull(icons.detail());
		assertNotNull(icons.print());
		assertNotNull(icons.clearSelection());
		assertNotNull(icons.edit());
		assertNotNull(icons.summary());
		assertNotNull(icons.editPanel());
		assertNotNull(icons.dependencies());
		assertNotNull(icons.settings());
		assertNotNull(icons.search());
		assertNotNull(icons.calendar());
		assertNotNull(icons.editText());
		assertNotNull(icons.logo());
	}
}
