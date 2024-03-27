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
 * Copyright (c) 2021 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui.icon;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.Ikon;

import javax.swing.ImageIcon;

import static org.junit.jupiter.api.Assertions.*;

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
		assertNotNull(icons.refreshRequired());
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
		ImageIcon logo12 = icons.logo(12);
		assertNotNull(logo12);
		assertSame(logo12, icons.logo(12));
		assertThrows(NullPointerException.class, () -> icons.add((Ikon[]) null));
		assertThrows(NullPointerException.class, () -> icons.add(null, FrameworkIkon.SETTINGS));
		assertThrows(IllegalArgumentException.class, () -> icons.add(FrameworkIkon.SETTINGS));
	}
}
