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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.demos.employees.ui;

import is.codion.common.user.User;
import is.codion.demos.employees.domain.Employees;
import is.codion.demos.employees.domain.Employees.Department;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.test.EntityEditPanelTestUnit;

import org.junit.jupiter.api.Test;

public class DepartmentEditPanelTest extends EntityEditPanelTestUnit {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	public DepartmentEditPanelTest() {
		super(new SwingEntityEditModel(Department.TYPE, EntityConnectionProvider.builder()
										.domainType(Employees.DOMAIN)
										.clientType(DepartmentEditPanel.class.getName())
										.user(UNIT_TEST_USER)
										.build()),
						DepartmentEditPanel::new);
	}

	@Test
	void initialize() {
		testInitialize();
	}
}
