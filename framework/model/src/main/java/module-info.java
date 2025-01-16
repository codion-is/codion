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
 * Framework application model classes, such as:
 * <ul>
 * <li>{@link is.codion.framework.model.EntityModel}
 * <li>{@link is.codion.framework.model.EntityEditModel}
 * <li>{@link is.codion.framework.model.EntityTableModel}
 * <li>{@link is.codion.framework.model.EntityTableConditionModel}
 * <li>{@link is.codion.framework.model.EntityQueryModel}
 * <li>{@link is.codion.framework.model.EntityApplicationModel}
 * </ul>
 */
module is.codion.framework.model {
	requires org.slf4j;
	requires transitive is.codion.common.model;
	requires transitive is.codion.framework.db.core;

	exports is.codion.framework.model;
}