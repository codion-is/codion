/*
 * Copyright (c) 2021 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Employee;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.model.SwingEntityTreeModel;

import static is.codion.swing.framework.model.SwingEntityTreeModel.swingEntityTreeModel;

public final class EmployeeTableModel extends SwingEntityTableModel {

  private final SwingEntityTreeModel treeModel;

  public EmployeeTableModel(EntityConnectionProvider connectionProvider) {
    super(Employee.TYPE, connectionProvider);
    this.treeModel = swingEntityTreeModel(this, Employee.REPORTSTO_FK);
  }

  public SwingEntityTreeModel treeModel() {
    return treeModel;
  }
}
