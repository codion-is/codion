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
 * World demo.
 */
module is.codion.demos.world {
	requires is.codion.swing.framework.ui;
	requires is.codion.plugin.jasperreports;
	requires is.codion.framework.json.domain;
	requires net.sf.jasperreports.core;
	requires org.jfree.jfreechart;
	requires org.jxmapviewer.jxmapviewer2;
	requires org.json;
	requires is.codion.plugin.flatlaf.intellij.themes;

	exports is.codion.demos.world.domain;
	exports is.codion.demos.world.model
					to is.codion.swing.framework.ui;
	exports is.codion.demos.world.ui
					to is.codion.swing.framework.ui;
	//for loading reports from classpath
	opens is.codion.demos.world.model;

	provides is.codion.framework.domain.Domain
					with is.codion.demos.world.domain.WorldImpl;
	// tag::customSerializer[]
	provides is.codion.framework.json.domain.EntityObjectMapperFactory
					with is.codion.demos.world.domain.WorldObjectMapperFactory;
	// end::customSerializer[]
}