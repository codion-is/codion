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
package is.codion.plugin.flatlaf.intellij.scaler;

import is.codion.swing.common.ui.scaler.Scaler;

import static java.util.Objects.requireNonNull;

public final class UIScaler implements Scaler {

	public UIScaler() {}

	@Override
	public void apply() {
		if (SCALING.isNot(100)) {
			System.setProperty("flatlaf.uiScale", String.valueOf(SCALING.getOrThrow() / 100f));
		}
	}

	@Override
	public boolean supports(String lookAndFeelClassName) {
		return requireNonNull(lookAndFeelClassName).startsWith("is.codion.plugin.flatlaf.intellij");
	}
}
