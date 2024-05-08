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
 * Copyright (c) 2011 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.model.EntitySearchModel;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;

import static is.codion.swing.framework.model.SwingForeignKeyConditionModel.swingForeignKeyConditionModel;
import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public class EntityComboBoxConditionModelTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	@Test
	void searchEntitiesComboBoxModel() throws DatabaseException {
		SwingForeignKeyConditionModel conditionModel = swingForeignKeyConditionModel(Employee.DEPARTMENT_FK,
						foreignKey -> {
							EntityComboBoxModel comboBoxModel = new EntityComboBoxModel(foreignKey.referencedType(), CONNECTION_PROVIDER);
							comboBoxModel.setNullCaption(FilterComboBoxModel.COMBO_BOX_NULL_CAPTION.get());

							return comboBoxModel;
			},
						foreignKey -> EntitySearchModel.builder(foreignKey.referencedType(), CONNECTION_PROVIDER).build());
		EntityComboBoxModel equalComboBoxModel = conditionModel.equalComboBoxModel();
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		equalComboBoxModel.setSelectedItem(sales);
		Entity searchEntity = conditionModel.getEqualValue();
		assertSame(sales, searchEntity);
		equalComboBoxModel.refresh();
		assertEquals(sales, equalComboBoxModel.selectedValue());

		conditionModel.setEqualValue(null);
		assertTrue(equalComboBoxModel.nullSelected());
		conditionModel.setEqualValue(sales);
		assertEquals(equalComboBoxModel.getSelectedItem(), sales);

		equalComboBoxModel.setSelectedItem(null);
	}

	@Test
	void inSearchModel() throws DatabaseException {
		SwingForeignKeyConditionModel conditionModel = swingForeignKeyConditionModel(Employee.DEPARTMENT_FK,
						foreignKey -> new EntityComboBoxModel(foreignKey.referencedType(), CONNECTION_PROVIDER),
						foreignKey -> EntitySearchModel.builder(foreignKey.referencedType(), CONNECTION_PROVIDER).build());
		EntitySearchModel inSearchModel = conditionModel.inSearchModel();
		Entity sales = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("SALES"));
		inSearchModel.entity().set(sales);
		Collection<Entity> searchEntities = conditionModel.getInValues();
		assertEquals(1, searchEntities.size());
		assertTrue(searchEntities.contains(sales));
		Entity accounting = CONNECTION_PROVIDER.connection().selectSingle(Department.NAME.equalTo("ACCOUNTING"));
		List<Entity> salesAccounting = asList(sales, accounting);
		inSearchModel.entities().set(salesAccounting);
		assertTrue(conditionModel.getInValues().contains(sales));
		assertTrue(conditionModel.getInValues().contains(accounting));
		searchEntities = conditionModel.getInValues();
		assertEquals(2, searchEntities.size());
		assertTrue(searchEntities.contains(sales));
		assertTrue(searchEntities.contains(accounting));
	}
}
