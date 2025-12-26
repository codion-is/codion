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
package is.codion.swing.common.ui.icon;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static is.codion.swing.common.ui.icon.SVGIcon.svgIcon;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class SVGIconsTest {

	public static final SVGIcon SMALL_ICON = svgIcon(SVGIconsTest.class.getResource("alert.svg"), 10, Color.BLACK);
	public static final SVGIcon LARGE_ICON = svgIcon(SVGIconsTest.class.getResource("alert.svg"), 16, Color.BLACK);

	@Test
	void test() {
		SVGIcons icons = SVGIcons.svgIcons(16);
		assertThrows(IllegalArgumentException.class, () -> icons.get("alert.svg"));
		icons.put("alert", SVGIconsTest.class.getResource("alert.svg"));
		icons.put("foundation", SVGIconsTest.class.getResource("foundation.svg"));
		assertThrows(IllegalArgumentException.class, () -> icons.put("foundation", SVGIconsTest.class.getResource("alert.svg")));
		assertNotNull(icons.get("alert"));
		assertNotNull(icons.get("foundation"));
	}
}
