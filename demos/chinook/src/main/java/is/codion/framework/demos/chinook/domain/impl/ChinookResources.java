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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.domain.impl;

import is.codion.common.resource.Resources;
import is.codion.framework.i18n.FrameworkMessages;

import java.util.Locale;

/**
 * Replace the english insert caption/mnemonic Add/A with Insert/I.
 */
public final class ChinookResources implements Resources {

	private static final String FRAMEWORK_MESSAGES =
					FrameworkMessages.class.getName();

	private final boolean english = Locale.getDefault()
					.equals(new Locale("en", "EN"));

	@Override
	public String getString(String baseBundleName, String key, String defaultString) {
		if (english && baseBundleName.equals(FRAMEWORK_MESSAGES)) {
			return switch (key) {
				case "insert" -> "Insert";
				case "insert_mnemonic" -> "I";
				default -> defaultString;
			};
		}

		return defaultString;
	}
}
