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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
/**
 * <p>Reactive classes used throughout.
 * <ul>
 * <li>{@link is.codion.common.reactive.observer.Observer}
 * <li>{@link is.codion.common.reactive.observer.Observable}
 * <li>{@link is.codion.common.reactive.event.Event}
 * <li>{@link is.codion.common.reactive.state.State}
 * <li>{@link is.codion.common.reactive.state.ObservableState}
 * <li>{@link is.codion.common.reactive.value.Value}
 * </ul>
 * <p>
 */
@org.jspecify.annotations.NullMarked
module is.codion.common.reactive {
	requires transitive org.jspecify;

	exports is.codion.common.reactive.event;
	exports is.codion.common.reactive.observer;
	exports is.codion.common.reactive.state;
	exports is.codion.common.reactive.value;
}