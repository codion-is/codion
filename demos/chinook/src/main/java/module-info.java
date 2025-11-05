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
 * Chinook demo.
 */
module is.codion.framework.demos.chinook {
	requires is.codion.common.rmi;
	requires is.codion.framework.db.local;
	requires is.codion.framework.db.http;
	requires is.codion.framework.db.rmi;
	requires is.codion.framework.json.domain;
	requires is.codion.tools.loadtest.ui;
	requires is.codion.swing.framework.ui;
	requires is.codion.plugin.jasperreports;
	requires net.sf.jasperreports.core;
	requires org.jfree.jfreechart;
	requires com.formdev.flatlaf.extras;
	requires is.codion.plugin.flatlaf.intellij.themes;
	requires org.kordamp.ikonli.foundation;
	requires is.codion.tools.swing.mcp;

	exports is.codion.demos.chinook.model
					to is.codion.swing.framework.model, is.codion.swing.framework.ui;
	exports is.codion.demos.chinook.ui
					to is.codion.swing.framework.ui;
	exports is.codion.demos.chinook.tutorial
					to is.codion.framework.db.local;

	provides is.codion.framework.domain.Domain
					with is.codion.demos.chinook.domain.ChinookImpl;
	provides is.codion.common.rmi.server.Authenticator
					with is.codion.demos.chinook.server.ChinookAuthenticator;
	provides is.codion.common.utilities.resource.Resources
					with is.codion.demos.chinook.i18n.ChinookResources;
	// tag::entityObjectMapper[]
	provides is.codion.framework.json.domain.EntityObjectMapperFactory
					with is.codion.demos.chinook.domain.ChinookObjectMapperFactory;
	// end::entityObjectMapper[]
}