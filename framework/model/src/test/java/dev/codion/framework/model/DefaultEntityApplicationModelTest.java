/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.model;

import dev.codion.framework.model.tests.AbstractEntityApplicationModelTest;
import dev.codion.framework.model.tests.TestDomain;

public final class DefaultEntityApplicationModelTest extends AbstractEntityApplicationModelTest<DefaultEntityModelTest.TestEntityModel,
        DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

  @Override
  protected DefaultEntityModelTest.TestEntityModel createDepartmentModel() {
    final DefaultEntityModelTest.TestEntityModel deptModel = new DefaultEntityModelTest.TestEntityModel(new DefaultEntityModelTest.TestEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider()));
    final DefaultEntityModelTest.TestEntityModel empModel = new DefaultEntityModelTest.TestEntityModel(new DefaultEntityModelTest.TestEntityEditModel(TestDomain.T_EMP, getConnectionProvider()));
    deptModel.addDetailModel(empModel);
    deptModel.addLinkedDetailModel(empModel);

    return deptModel;
  }
}
