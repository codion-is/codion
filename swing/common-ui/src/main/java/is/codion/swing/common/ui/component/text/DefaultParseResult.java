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
package is.codion.swing.common.ui.component.text;

class DefaultParseResult<T> implements Parser.ParseResult<T> {

	private final String text;
	private final T value;
	private final boolean successful;

	DefaultParseResult(String text, T value, boolean successful) {
		this.text = text;
		this.value = value;
		this.successful = successful;
	}

	@Override
	public final String text() {
		return text;
	}

	@Override
	public final T value() {
		return value;
	}

	@Override
	public final boolean successful() {
		return successful;
	}
}
