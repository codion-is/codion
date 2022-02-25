/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model;

import is.codion.common.event.EventListener;
import is.codion.common.state.StateObserver;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.model.test.AbstractEntityModelTest;
import is.codion.framework.model.test.TestDomain;

import java.util.List;

public class DefaultEntityModelTest extends AbstractEntityModelTest<DefaultEntityModelTest.TestEntityModel,
        DefaultEntityModelTest.TestEntityEditModel, DefaultEntityModelTest.TestEntityTableModel> {

  @Override
  protected TestEntityModel createDepartmentModel() {
    TestEntityModel deptModel = new TestEntityModel(new TestEntityEditModel(TestDomain.T_DEPARTMENT, getConnectionProvider()));
    TestEntityModel empModel = new TestEntityModel(new TestEntityEditModel(TestDomain.T_EMP, getConnectionProvider()));
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

    public TestEntityEditModel(final EntityType entityType, final EntityConnectionProvider connectionProvider) {
      super(entityType, connectionProvider);
    }
    @Override
    public void addForeignKeyValues(final List<Entity> entities) {}
    @Override
    public void removeForeignKeyValues(final List<Entity> entities) {}
    @Override
    public void clear() {}
    @Override
    public StateObserver getRefreshingObserver() {
      return null;
    }
    @Override
    public void addRefreshingObserver(final StateObserver refreshingObserver) {}
    @Override
    public void addRefreshListener(final EventListener listener) {}
    @Override
    public void removeRefreshListener(final EventListener listener) {}
  }

  public static final class TestEntityModel extends DefaultEntityModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel> {
    public TestEntityModel(final TestEntityEditModel editModel) {
      super(editModel);
    }
  }

  public interface TestEntityTableModel extends EntityTableModel<TestEntityEditModel> {}
}
