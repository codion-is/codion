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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
/**
 * JSON serialization for domain related classes.
 * <ul>
 * <li>{@link is.codion.framework.json.domain.EntityObjectMapper}
 * <li>{@link is.codion.framework.json.domain.EntityObjectMapperFactory}
 * </ul>
 * @uses is.codion.framework.json.domain.EntityObjectMapperFactory
 */
module is.codion.framework.json.domain {
	requires transitive com.fasterxml.jackson.databind;
	requires transitive com.fasterxml.jackson.datatype.jsr310;
	requires is.codion.framework.domain;

	exports is.codion.framework.json.domain;

	uses is.codion.framework.json.domain.EntityObjectMapperFactory;
}