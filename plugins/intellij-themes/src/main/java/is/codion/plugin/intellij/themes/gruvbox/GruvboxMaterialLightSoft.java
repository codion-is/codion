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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.plugin.intellij.themes.gruvbox;

import com.formdev.flatlaf.IntelliJTheme;

import static is.codion.plugin.intellij.themes.ThemeLoader.load;

public final class GruvboxMaterialLightSoft extends IntelliJTheme.ThemeLaf {

	public GruvboxMaterialLightSoft() {
		super(load(GruvboxMaterialLightSoft.class.getResourceAsStream("gruvbox-material-light-soft.theme.json")));
	}
}
