/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.model;

public final class DefaultEntityApplicationModelTest extends AbstractEntityApplicationModelTest<DefaultEntityModelTest.TestEntityModel,
        DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

  @Override
  protected DefaultEntityModelTest.TestEntityModel createDepartmentModel() {
    final DefaultEntityModelTest.TestEntityModel deptModel = new DefaultEntityModelTest.TestEntityModel(new DefaultEntityModelTest.TestEntityEditModel(TestDomain.T_DEPARTMENT, CONNECTION_PROVIDER));
    final DefaultEntityModelTest.TestEntityModel empModel = new DefaultEntityModelTest.TestEntityModel(new DefaultEntityModelTest.TestEntityEditModel(TestDomain.T_EMP, CONNECTION_PROVIDER));
    deptModel.addDetailModel(empModel);
    deptModel.addLinkedDetailModel(empModel);

    return deptModel;
  }
}
