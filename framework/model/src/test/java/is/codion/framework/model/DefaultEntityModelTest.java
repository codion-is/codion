/*
 * Copyright (c) 2018 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.model.test.AbstractEntityModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import java.util.Collection;

public class DefaultEntityModelTest extends AbstractEntityModelTest<DefaultEntityModelTest.TestEntityModel,
        DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

  @Override
  protected TestEntityModel createDepartmentModel() {
    TestEntityModel deptModel = new TestEntityModel(new TestEntityEditModel(Department.TYPE, connectionProvider()));
    TestEntityModel empModel = new TestEntityModel(new TestEntityEditModel(Employee.TYPE, connectionProvider()));
    deptModel.addDetailModel(empModel).active().set(true);

    return deptModel;
  }

  @Override
  protected TestEntityModel createDepartmentModelWithoutDetailModel() {
    return new TestEntityModel(new TestEntityEditModel(Department.TYPE, connectionProvider()));
  }

  @Override
  protected TestEntityModel createEmployeeModel() {
    return new TestEntityModel(new TestEntityEditModel(Employee.TYPE, connectionProvider()));
  }

  public static final class TestEntityEditModel extends AbstractEntityEditModel {

    public TestEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
      super(entityType, connectionProvider);
    }
    @Override
    public void add(ForeignKey foreignKey, Collection<Entity> entities) {}
    @Override
    public void remove(ForeignKey foreignKey, Collection<Entity> entities) {}
  }

  public static final class TestEntityModel extends DefaultEntityModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel> {
    public TestEntityModel(TestEntityEditModel editModel) {
      super(editModel);
    }
  }

  public interface TestEntityTableModel extends EntityTableModel<TestEntityEditModel> {}
}
