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
 * Configuration values:
 * <ul>
 * <li>{@link is.codion.common.utilities.Text#COLLATOR_LANGUAGE}
 * </ul>
 * @uses is.codion.common.utilities.logging.LoggerProxy
 * @uses is.codion.common.utilities.resource.Resources
 */
@org.jspecify.annotations.NullMarked
module is.codion.common.utilities {
	requires transitive is.codion.common.reactive;

	exports is.codion.common.utilities;
	exports is.codion.common.utilities.exceptions;
	exports is.codion.common.utilities.format;
	exports is.codion.common.utilities.item;
	exports is.codion.common.utilities.logging;
	exports is.codion.common.utilities.property;
	exports is.codion.common.utilities.proxy;
	exports is.codion.common.utilities.resource;
	exports is.codion.common.utilities.scheduler;
	exports is.codion.common.utilities.user;
	exports is.codion.common.utilities.version;

	uses is.codion.common.utilities.logging.LoggerProxy;
	uses is.codion.common.utilities.resource.Resources;
}