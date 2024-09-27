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
 * Shared model classes, such as:
 * <li>{@link is.codion.common.model.UserPreferences}
 * <li>{@link is.codion.common.model.CancelException}
 * <li>{@link is.codion.common.model.FilterModel}
 * <li>{@link is.codion.common.model.selection.SingleSelection}
 * <li>{@link is.codion.common.model.selection.Selection}
 * <li>{@link is.codion.common.model.loadtest.LoadTest}
 * <li>{@link is.codion.common.model.condition.ConditionModel}
 * <li>{@link is.codion.common.model.condition.TableConditionModel}
 * <li>{@link is.codion.common.model.summary.SummaryModel}
 * <li>{@link is.codion.common.model.summary.TableSummaryModel}
 */
module is.codion.common.model {
	requires org.slf4j;
	requires java.prefs;
	requires transitive is.codion.common.core;

	exports is.codion.common.model;
	exports is.codion.common.model.condition;
	exports is.codion.common.model.loadtest;
	exports is.codion.common.model.randomizer;
	exports is.codion.common.model.selection;
	exports is.codion.common.model.summary;
}