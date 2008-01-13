/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.beans.combo;

import org.jminor.framework.client.dbprovider.IEntityDbProvider;
import org.jminor.framework.client.model.combobox.EntityComboBoxModel;
import org.jminor.framework.demos.empdept.model.EmpDept;

public class DepartmentComboBoxModel extends EntityComboBoxModel {

  public DepartmentComboBoxModel(final IEntityDbProvider dbProvider) {
    super(dbProvider, EmpDept.T_DEPARTMENT);
  }
}
