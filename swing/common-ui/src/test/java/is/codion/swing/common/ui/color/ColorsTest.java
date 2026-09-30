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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.color;

import org.junit.jupiter.api.Test;

import java.awt.Color;

import static is.codion.swing.common.ui.color.Colors.shade;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

public final class ColorsTest {

	@Test
	void shadeLight() {
		// towards black
		assertEquals(new Color(240, 240, 240), shade(Color.WHITE));
		assertEquals(new Color(128, 128, 128), shade(Color.WHITE, 0.5));
	}

	@Test
	void shadeDark() {
		// towards white
		assertEquals(new Color(15, 15, 15), shade(Color.BLACK));
		assertEquals(new Color(81, 84, 86), shade(new Color(70, 73, 75)));
	}

	@Test
	void shadeByLuminance() {
		// pure blue is dark and pure yellow light, by perceived luminance, although both have one channel at zero
		assertEquals(new Color(15, 15, 255), shade(Color.BLUE));
		assertEquals(new Color(240, 240, 0), shade(Color.YELLOW));
	}

	@Test
	void shadeAlpha() {
		assertEquals(100, shade(new Color(255, 255, 255, 100)).getAlpha());
	}

	@Test
	void shadeFraction() {
		assertThrows(IllegalArgumentException.class, () -> shade(Color.WHITE, 0));
		assertThrows(IllegalArgumentException.class, () -> shade(Color.WHITE, 1));
	}
}
