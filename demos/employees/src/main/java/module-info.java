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
 * Employees demo.
 */
module is.codion.demos.employees {
	requires net.sf.jasperreports.core;
	requires is.codion.common.rmi;
	requires is.codion.framework.db.http;
	requires is.codion.framework.domain.test;
	requires is.codion.framework.json.domain;
	requires is.codion.tools.loadtest.ui;
	requires is.codion.swing.framework.ui;
	requires is.codion.framework.server;
	requires is.codion.plugin.jasperreports;
	requires is.codion.plugin.flatlaf.intellij.themes;
	requires is.codion.tools.swing.robot;
	requires is.codion.tools.swing.mcp;

	exports is.codion.demos.employees.domain
					to is.codion.framework.domain, is.codion.framework.db.local;
	exports is.codion.demos.employees.model
					to is.codion.swing.framework.ui;
	exports is.codion.demos.employees.ui
					to is.codion.swing.framework.ui;
	exports is.codion.demos.employees.server
					to java.rmi;
	//for loading of reports from classpath
	opens is.codion.demos.employees.domain
					to is.codion.plugin.jasperreports;

	provides is.codion.framework.domain.Domain
					with is.codion.demos.employees.domain.Employees;
}