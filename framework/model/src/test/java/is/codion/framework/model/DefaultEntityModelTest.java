/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.model.table.FilteredTableModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.property.Property;
import is.codion.framework.model.tests.AbstractEntityModelTest;
import is.codion.framework.model.tests.TestDomain;

import java.util.List;

public class DefaultEntityModelTest extends AbstractEntityModelTest<DefaultEntityModelTest.TestEntityModel,
        DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

  @Override
  protected TestEntityModel createDepartmentModel() {
    final TestEntityModel deptModel = new TestEntityModel(new TestEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider()));
    final TestEntityModel empModel = new TestEntityModel(new TestEntityEditModel(TestDomain.T_EMP, getConnectionProvider()));
    deptModel.addDetailModel(empModel);
    deptModel.addLinkedDetailModel(empModel);

    return deptModel;
  }

  @Override
  protected TestEntityModel createDepartmentModelWithoutDetailModel() {
    return new TestEntityModel(new TestEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider()));
  }

  @Override
  protected TestEntityModel createEmployeeModel() {
    return new TestEntityModel(new TestEntityEditModel(TestDomain.T_EMP, getConnectionProvider()));
  }

  @Override
  protected TestEntityEditModel createDepartmentEditModel() {
    return new TestEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider());
  }

  @Override
  protected TestEntityTableModel createEmployeeTableModel() {
    return null;
  }

  @Override
  protected TestEntityTableModel createDepartmentTableModel() {
    return null;
  }

  public static final class TestEntityEditModel extends DefaultEntityEditModel {

    public TestEntityEditModel(final EntityType<?> entityType, final EntityConnectionProvider connectionProvider) {
      super(entityType, connectionProvider);
    }
    @Override
    public void addForeignKeyValues(final List<Entity> entities) {}
    @Override
    public void removeForeignKeyValues(final List<Entity> entities) {}
    @Override
    public void clear() {}
  }

  public static final class TestEntityModel extends DefaultEntityModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel> {
    public TestEntityModel(final TestEntityEditModel editModel) {
      super(editModel, null);
    }
  }

  public interface TestEntityTableModel extends EntityTableModel<TestEntityEditModel>, FilteredTableModel<Entity, Property<?>, Object> {}
}
