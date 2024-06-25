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
 * Framework EntityServer monitor.
 */
module is.codion.swing.framework.server.monitor {
	requires org.slf4j;
	requires org.jfree.jfreechart;
	requires com.formdev.flatlaf.intellijthemes;
	requires is.codion.framework.server;
	requires is.codion.swing.common.ui;

	exports is.codion.swing.framework.server.monitor;
	exports is.codion.swing.framework.server.monitor.ui;
}