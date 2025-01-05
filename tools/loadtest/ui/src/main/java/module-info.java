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
 * Common Swing loadtest UI.
 * <ul>
 * <li>{@link is.codion.tools.loadtest.ui.LoadTestPanel}
 * </ul>
 */
module is.codion.tools.loadtest.ui {
	requires org.jfree.jfreechart;
	requires is.codion.plugin.flatlaf;
	requires transitive is.codion.tools.loadtest.model;
	requires transitive is.codion.swing.common.ui;

	exports is.codion.tools.loadtest.ui;
}