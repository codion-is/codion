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
 * Base classes for unit testing framework application model classes:<br>
 * <br>
 * {@link is.codion.framework.model.test.AbstractEntityModelTest}<br>
 * {@link is.codion.framework.model.test.AbstractEntityTableModelTest}<br>
 * {@link is.codion.framework.model.test.AbstractEntityApplicationModelTest}<br>
 */
module is.codion.framework.model.test {
	requires org.slf4j;
	requires org.junit.jupiter.api;
	requires is.codion.framework.db.local;
	requires transitive is.codion.common.model;
	requires transitive is.codion.framework.db.core;
	requires transitive is.codion.framework.model;

	exports is.codion.framework.model.test;
}