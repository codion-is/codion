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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.plugin.jasperreports;

import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.plugin.jasperreports.TestDomain.Department;

import net.sf.jasperreports.engine.JRField;
import net.sf.jasperreports.engine.JRPropertiesHolder;
import net.sf.jasperreports.engine.JRPropertiesMap;
import net.sf.jasperreports.engine.JRPropertyExpression;
import org.junit.jupiter.api.Test;

import java.util.List;

import static java.util.Collections.singletonList;

public class JasperReportsDataSourceTest {

	private static final Entities ENTITIES = new TestDomain().entities();

	@Test
	void iterator() throws Exception {
		Entity department = ENTITIES.entity(Department.TYPE)
						.with(Department.ID, 10)
						.with(Department.NAME, "name")
						.with(Department.LOCATION, "none")
						.build();
		EntityDefinition definition = ENTITIES.definition(Department.TYPE);
		List<Entity> entities = singletonList(department);
		JasperReportsDataSource<Entity> source =
						new JasperReportsDataSource<>(entities.iterator(), (entity, field) ->
										entity.get(definition.attributes().get(field.getName())));
		while (source.next()) {
			JRField field = new TestField(Department.NAME.name());
			source.getFieldValue(field);
		}
	}

	private static class TestField implements JRField {
		private final String name;

		TestField(String name) {this.name = name;}

		@Override
		public String getName() {return name;}

		@Override
		public String getDescription() {return null;}

		@Override
		public void setDescription(String s) {}

		@Override
		public Class<String> getValueClass() {return null;}

		@Override
		public String getValueClassName() {return null;}

		@Override
		public boolean hasProperties() {return false;}

		@Override
		public JRPropertiesMap getPropertiesMap() {return null;}

		@Override
		public JRPropertiesHolder getParentProperties() {return null;}

		@Override
		public Object clone() {return null;}

		@Override
		public JRPropertyExpression[] getPropertyExpressions() {return new JRPropertyExpression[0];}
	}
}
