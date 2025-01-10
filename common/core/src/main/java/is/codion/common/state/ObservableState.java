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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.common.state;

import is.codion.common.observable.Observable;

import org.jspecify.annotations.NonNull;

/**
 * Specifies an observable for a {@link State} instance.
 */
public interface ObservableState extends Observable<Boolean> {

	@Override
	@NonNull Boolean get();

	/**
	 * @return false
	 */
	@Override
	default boolean isNull() {
		return false;
	}

	/**
	 * @return false
	 */
	@Override
	default boolean isNullable() {
		return false;
	}

	/**
	 * @return A {@link ObservableState} instance that is always the reverse of this {@link ObservableState} instance
	 */
	ObservableState not();
}
