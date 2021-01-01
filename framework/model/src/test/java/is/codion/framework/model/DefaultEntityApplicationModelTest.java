/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.model.tests.AbstractEntityApplicationModelTest;
import is.codion.framework.model.tests.TestDomain;

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
