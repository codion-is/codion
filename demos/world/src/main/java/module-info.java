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
 * World demo.
 */
module is.codion.framework.demos.world {
	requires is.codion.swing.framework.ui;
	requires is.codion.plugin.jasperreports;
	requires is.codion.framework.json.domain;
	requires com.formdev.flatlaf;
	requires com.formdev.flatlaf.intellijthemes;
	requires org.kordamp.ikonli.foundation;
	requires jasperreports;
	requires org.jfree.jfreechart;
	requires org.jxmapviewer.jxmapviewer2;
	requires org.json;

	exports is.codion.framework.demos.world.domain;
	exports is.codion.framework.demos.world.model
					to is.codion.swing.framework.ui;
	exports is.codion.framework.demos.world.ui
					to is.codion.swing.framework.ui;
	//for loading reports from classpath
	opens is.codion.framework.demos.world.model;
	//for accessing default methods in EntityType interfaces
	opens is.codion.framework.demos.world.domain.api
					to is.codion.framework.domain;

	provides is.codion.framework.domain.Domain
					with is.codion.framework.demos.world.domain.WorldImpl;
	// tag::customSerializer[]
	provides is.codion.framework.json.domain.EntityObjectMapperFactory
					with is.codion.framework.demos.world.domain.api.WorldObjectMapperFactory;
	// end::customSerializer[]
}