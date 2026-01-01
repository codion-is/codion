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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.plugin.flatlaf.intellij.themes.gerry;

import com.formdev.flatlaf.IntelliJTheme;

import static is.codion.plugin.flatlaf.intellij.themes.ThemeLoader.load;

/**
 * https://github.com/gerry-themes/gerry-themes/blob/master/resources/light/gerryLight.theme.json
 */
public final class GerryLight extends IntelliJTheme.ThemeLaf {

	public GerryLight() {
		super(load(GerryLight.class.getResourceAsStream("gerryLight.theme.json")));
	}
}
