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
package is.codion.framework.demos.employees.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;

import java.util.Objects;

// tag::constructor[]
public final class EmployeeEditModel extends SwingEntityEditModel {

  public EmployeeEditModel(EntityConnectionProvider connectionProvider) {
    super(Employees.Employee.TYPE, connectionProvider);
    initializeComboBoxModels(Employees.Employee.MGR_FK, Employees.Employee.DEPARTMENT_FK);
    bindEvents();
  }
  // end::constructor[]

  // tag::createForeignKeyComboBox[]
  /** Providing a custom ComboBoxModel for the manager attribute, which only shows managers and the president */
  @Override
  public EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
    EntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKey);
    if (foreignKey.equals(Employees.Employee.MGR_FK)) {
      //Customize the null value caption so that it displays 'None'
      //instead of the default '-' character
      comboBoxModel.setNullCaption("None");
      //we do not want filtering to remove a value that is selected
      //and thereby change the selection, see bindEvents() below
      comboBoxModel.filterSelectedItem().set(false);
      //Only select the president and managers from the database
      comboBoxModel.condition().set(() ->
              Employees.Employee.JOB.in("Manager", "President"));
    }

    return comboBoxModel;
  }
  // end::createForeignKeyComboBox[]

  // tag::bindEvents[]
  private void bindEvents() {
    //Refresh the manager ComboBoxModel when an employee is added, deleted or updated,
    //in case a new manager got hired, fired or promoted
    addInsertUpdateOrDeleteListener(() -> foreignKeyComboBoxModel(Employees.Employee.MGR_FK).refresh());
    //Filter the manager ComboBoxModel so that only managers from the selected department are shown,
    //this filtering happens each time the department value is changed, either when an employee is
    //selected or the department combo box selection changes
    addValueListener(Employees.Employee.DEPARTMENT_FK, department -> {
      //only show managers from the same department as the selected employee and hide the currently
      //selected employee to prevent an employee from being made her own manager
      foreignKeyComboBoxModel(Employees.Employee.MGR_FK).includeCondition().set(manager ->
              Objects.equals(manager.referencedEntity(Employees.Employee.DEPARTMENT_FK), department)
                      && !Objects.equals(manager, entity()));
    });
  }
}
// end::bindEvents[]