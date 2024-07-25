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
 * Copyright (c) 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.common.model.loadtest.LoadTest.Scenario.Performer;
import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.framework.model.EntityTableModel;

import java.util.Random;

abstract class AbstractPerformer implements Performer<EmployeesAppModel> {

	private static final Random RANDOM = new Random();

	protected static void selectRandomRow(EntityTableModel<?> tableModel) {
		if (tableModel.rowCount() > 0) {
			tableModel.selectionModel().setSelectedIndex(RANDOM.nextInt(tableModel.rowCount()));
		}
	}
}
