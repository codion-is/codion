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
package is.codion.swing.common.ui.icon;

import org.junit.jupiter.api.Test;
import org.kordamp.ikonli.foundation.Foundation;

import java.awt.Color;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class IconsTest {

	@Test
	void test() {
		Icons icons = Icons.icons();
		assertThrows(IllegalArgumentException.class, () -> icons.icon(Foundation.ALERT));
		icons.add(Foundation.ALERT, Foundation.FOUNDATION);
		assertThrows(IllegalArgumentException.class, () -> icons.add(Foundation.FOUNDATION));
		assertNotNull(icons.icon(Foundation.ALERT));
		assertNotNull(icons.icon(Foundation.FOUNDATION));
		Icons.ICON_COLOR.set(Color.RED);
	}
}
