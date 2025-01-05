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
package is.codion.plugin.flatlaf.intellij.themes;

import com.formdev.flatlaf.IntelliJTheme;

import java.io.IOException;
import java.io.InputStream;

import static java.util.Objects.requireNonNull;

public final class ThemeLoader {

	private ThemeLoader() {}

	public static IntelliJTheme load(InputStream inputStream) {
		requireNonNull(inputStream);
		try {
			return new IntelliJTheme(inputStream);
		}
		catch (IOException e) {
			throw new RuntimeException(e);
		}
	}
}
