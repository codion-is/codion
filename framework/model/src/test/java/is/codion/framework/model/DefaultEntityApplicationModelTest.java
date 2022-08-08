/*
 * Copyright (c) 2018 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.model.test.AbstractEntityApplicationModelTest;
import is.codion.framework.model.test.TestDomain;

public final class DefaultEntityApplicationModelTest extends AbstractEntityApplicationModelTest<DefaultEntityModelTest.TestEntityModel,
        DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

  @Override
  protected DefaultEntityModelTest.TestEntityModel createDepartmentModel() {
    DefaultEntityModelTest.TestEntityModel deptModel = new DefaultEntityModelTest.TestEntityModel(
            new DefaultEntityModelTest.TestEntityEditModel(TestDomain.T_DEPARTMENT, connectionProvider()));
    DefaultEntityModelTest.TestEntityModel empModel = new DefaultEntityModelTest.TestEntityModel(
            new DefaultEntityModelTest.TestEntityEditModel(TestDomain.T_EMP, connectionProvider()));
    deptModel.addDetailModel(empModel);
    deptModel.addLinkedDetailModel(empModel);

    return deptModel;
  }
}
