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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
/**
 * <p>Common classes used throughout.
 * <ul>
 * <li>{@link is.codion.common.observable.Observer}
 * <li>{@link is.codion.common.observable.Observable}
 * <li>{@link is.codion.common.event.Event}
 * <li>{@link is.codion.common.state.State}
 * <li>{@link is.codion.common.state.ObservableState}
 * <li>{@link is.codion.common.value.Value}
 * </ul>
 * <p>
 * Configuration values:
 * <ul>
 * <li>{@link is.codion.common.Text#DEFAULT_COLLATOR_LANGUAGE}
 * </ul>
 * @uses is.codion.common.logging.LoggerProxy
 * @uses is.codion.common.resource.Resources
 */
@org.jspecify.annotations.NullMarked
module is.codion.common.core {
	requires transitive org.jspecify;
	exports is.codion.common;
	exports is.codion.common.event;
	exports is.codion.common.format;
	exports is.codion.common.item;
	exports is.codion.common.logging;
	exports is.codion.common.observable;
	exports is.codion.common.property;
	exports is.codion.common.proxy;
	exports is.codion.common.resource;
	exports is.codion.common.scheduler;
	exports is.codion.common.state;
	exports is.codion.common.user;
	exports is.codion.common.value;
	exports is.codion.common.version;

	uses is.codion.common.logging.LoggerProxy;
	uses is.codion.common.resource.Resources;
}