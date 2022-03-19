/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.model.test;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.event.EventDataListener;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.Key;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.DefaultEntityEditModel;
import is.codion.framework.model.DefaultEntityModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableModel;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

import static is.codion.framework.db.condition.Conditions.where;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityModel} subclasses.
 * @param <Model> the {@link EntityModel} type
 * @param <EditModel> the {@link EntityEditModel} type
 * @param <TableModel> the {@link EntityTableModel} type
 */
public abstract class AbstractEntityModelTest<Model extends DefaultEntityModel<Model, EditModel, TableModel>,
        EditModel extends DefaultEntityEditModel, TableModel extends EntityTableModel<EditModel>> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  protected static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .user(UNIT_TEST_USER)
          .domainClassName(TestDomain.class.getName())
          .build();

  private final EntityConnectionProvider connectionProvider;

  protected final Model departmentModel;

  protected AbstractEntityModelTest() {
    connectionProvider = CONNECTION_PROVIDER;
    departmentModel = createDepartmentModel();
  }

  @Test
  public void testUpdatePrimaryKey() throws DatabaseException, ValidationException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    departmentModel.getTableModel().refresh();
    EntityEditModel deptEditModel = departmentModel.getEditModel();
    TableModel deptTableModel = departmentModel.getTableModel();
    Key operationsKey = deptEditModel.getEntities().primaryKey(TestDomain.T_DEPARTMENT, 40);//operations
    deptTableModel.setSelectedByKey(singletonList(operationsKey));

    assertTrue(deptTableModel.getSelectionModel().isSelectionNotEmpty());
    deptEditModel.put(TestDomain.DEPARTMENT_ID, 80);
    assertFalse(deptTableModel.getSelectionModel().isSelectionEmpty());
    deptEditModel.update();

    assertFalse(deptTableModel.getSelectionModel().isSelectionEmpty());
    Entity operations = deptTableModel.getSelectionModel().getSelectedItem();
    assertEquals(80, operations.get(TestDomain.DEPARTMENT_ID));

    deptTableModel.setIncludeCondition(item ->
            !Objects.equals(80, item.get(TestDomain.DEPARTMENT_ID)));

    deptEditModel.setEntity(operations);
    deptEditModel.put(TestDomain.DEPARTMENT_ID, 40);
    deptEditModel.update();

    deptTableModel.filterContents();

    assertTrue(deptTableModel.getFilteredItems().isEmpty());
  }

  @Test
  public void testDetailModels() throws DatabaseException, ValidationException {
    //todo
  }

  @Test
  public void getDetailModelNotFound() {
    assertThrows(IllegalArgumentException.class, () -> departmentModel.getDetailModel(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void clear() {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    departmentModel.getTableModel().refresh();
    assertTrue(departmentModel.getTableModel().getRowCount() > 0);

    Model employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    employeeModel.getTableModel().refresh();
    assertTrue(employeeModel.getTableModel().getRowCount() > 0);

    departmentModel.clearDetailModels();
    assertEquals(0, employeeModel.getTableModel().getRowCount());

    departmentModel.clear();
    assertEquals(0, departmentModel.getTableModel().getRowCount());
  }

  @Test
  public void constructorNullTableModel() {
    assertThrows(NullPointerException.class, () -> new DefaultEntityModel<>((EntityTableModel) null));
  }

  @Test
  public void clearEditModelClearTableSelection() {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    departmentModel.getTableModel().refresh();
    departmentModel.getTableModel().getSelectionModel().setSelectedIndexes(asList(1, 2, 3));
    assertTrue(departmentModel.getTableModel().getSelectionModel().isSelectionNotEmpty());
    assertFalse(departmentModel.getEditModel().isEntityNew());
    departmentModel.getEditModel().setDefaultValues();
    assertTrue(departmentModel.getTableModel().getSelectionModel().isSelectionEmpty());
  }

  @Test
  public void test() throws Exception {
    assertNull(departmentModel.getMasterModel());
    assertNotNull(departmentModel.getEditModel());

    EventDataListener<Model> linkedListener = model -> {};
    departmentModel.addLinkedDetailModelAddedListener(linkedListener);
    departmentModel.addLinkedDetailModelRemovedListener(linkedListener);
    departmentModel.removeLinkedDetailModelAddedListener(linkedListener);
    departmentModel.removeLinkedDetailModelRemovedListener(linkedListener);
  }

  @Test
  public void detailModel() throws Exception {
    departmentModel.getDetailModel((Class<? extends Model>) departmentModel.getDetailModel(TestDomain.T_EMP).getClass());
    assertTrue(departmentModel.containsDetailModel(TestDomain.T_EMP));
    assertTrue(departmentModel.containsDetailModel(departmentModel.getDetailModel(TestDomain.T_EMP)));
    assertTrue(departmentModel.containsDetailModel((Class<? extends Model>) departmentModel.getDetailModel(TestDomain.T_EMP).getClass()));
    assertEquals(1, departmentModel.getDetailModels().size(), "Only one detail model should be in DepartmentModel");
    assertEquals(1, departmentModel.getLinkedDetailModels().size());

    departmentModel.getDetailModel(TestDomain.T_EMP);

    assertTrue(departmentModel.getLinkedDetailModels().contains(departmentModel.getDetailModel(TestDomain.T_EMP)));
    assertNotNull(departmentModel.getDetailModel(TestDomain.T_EMP));
    if (!departmentModel.containsTableModel()) {
      return;
    }

    departmentModel.getTableModel().refresh();
    departmentModel.getDetailModel(TestDomain.T_EMP).getTableModel().refresh();
    assertTrue(departmentModel.getDetailModel(TestDomain.T_EMP).getTableModel().getRowCount() > 0);

    EntityConnection connection = departmentModel.getConnectionProvider().getConnection();
    Entity department = connection.selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");

    departmentModel.getTableModel().getSelectionModel().setSelectedItem(department);

    List<Entity> salesEmployees = connection.select(where(TestDomain.EMP_DEPARTMENT_FK).equalTo(department));
    assertFalse(salesEmployees.isEmpty());
    departmentModel.getTableModel().getSelectionModel().setSelectedItem(department);
    List<Entity> employeesFromDetailModel =
            departmentModel.getDetailModel(TestDomain.T_EMP).getTableModel().getItems();
    assertTrue(salesEmployees.containsAll(employeesFromDetailModel), "Filtered list should contain all employees for department");
  }

  @Test
  public void addSameDetailModelTwice() {
    Model model = createDepartmentModelWithoutDetailModel();
    Model employeeModel = createEmployeeModel();
    assertThrows(IllegalArgumentException.class, () -> model.addDetailModels(employeeModel, employeeModel));
  }

  @Test
  public void addLinkedDetailModelWithoutAddingFirst() {
    Model model = createDepartmentModelWithoutDetailModel();
    Model employeeModel = createEmployeeModel();
    assertThrows(IllegalStateException.class, () -> model.addLinkedDetailModel(employeeModel));
  }

  @Test
  public void removeLinkedDetailModelWithoutAddingFirst() {
    Model model = createDepartmentModelWithoutDetailModel();
    Model employeeModel = createEmployeeModel();
    assertThrows(IllegalStateException.class, () -> model.removeLinkedDetailModel(employeeModel));
  }

  @Test
  public void addDetailModelDetailModelAlreadyHasMasterModel() {
    Model model = createDepartmentModelWithoutDetailModel();
    Model employeeModel = createEmployeeModel();
    employeeModel.setMasterModel(model);
    assertThrows(IllegalArgumentException.class, () -> model.addDetailModel(employeeModel));
  }

  @Test
  public void setMasterModel() {
    Model model = createDepartmentModelWithoutDetailModel();
    Model employeeModel = createEmployeeModel();
    employeeModel.setMasterModel(model);
    assertThrows(IllegalStateException.class, () -> employeeModel.setMasterModel(departmentModel));
  }

  @Test
  public void addRemoveLinkedDetailModel() {
    departmentModel.removeLinkedDetailModel(departmentModel.getDetailModel(TestDomain.T_EMP));
    assertTrue(departmentModel.getLinkedDetailModels().isEmpty());
    departmentModel.addLinkedDetailModel(departmentModel.getDetailModel(TestDomain.T_EMP));
    assertFalse(departmentModel.getLinkedDetailModels().isEmpty());
    assertTrue(departmentModel.getLinkedDetailModels().contains(departmentModel.getDetailModel(TestDomain.T_EMP)));
  }

  @Test
  public void filterOnMasterInsert() throws DatabaseException, ValidationException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    Model employeeModel = departmentModel.getDetailModel(TestDomain.T_EMP);
    employeeModel.setSearchOnMasterInsert(true);
    assertTrue(employeeModel.isSearchOnMasterInsert());
    EntityEditModel editModel = departmentModel.getEditModel();
    editModel.put(TestDomain.DEPARTMENT_ID, 100);
    editModel.put(TestDomain.DEPARTMENT_NAME, "Name");
    editModel.put(TestDomain.DEPARTMENT_LOCATION, "Loc");
    Entity inserted = editModel.insert();
    Collection<Entity> equalsValues = employeeModel.getTableModel().getTableConditionModel()
            .getConditionModel(TestDomain.EMP_DEPARTMENT_FK)
            .getEqualValues();
    assertEquals(inserted, equalsValues.iterator().next());
    editModel.delete();
  }

  @Test
  public void insertDifferentTypes() throws DatabaseException, ValidationException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    Entity dept = departmentModel.getConnectionProvider().getEntities().builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, -42)
            .with(TestDomain.DEPARTMENT_NAME, "Name")
            .with(TestDomain.DEPARTMENT_LOCATION, "Loc")
            .build();

    Entity emp = connectionProvider.getConnection().selectSingle(TestDomain.EMP_ID, 8).clearPrimaryKey();
    emp.put(TestDomain.EMP_NAME, "NewName");

    Model model = createDepartmentModelWithoutDetailModel();
    model.getEditModel().insert(asList(dept, emp));
    assertTrue(model.getTableModel().containsItem(dept));
    assertFalse(model.getTableModel().containsItem(emp));

    model.getEditModel().delete(asList(dept, emp));

    assertFalse(model.getTableModel().containsItem(dept));
  }

  protected final EntityConnectionProvider getConnectionProvider() {
    return connectionProvider;
  }

  /**
   * @return a EntityModel based on the department entity
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract Model createDepartmentModel();

  /**
   * @return a EntityModel based on the department entity, without detail models
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract Model createDepartmentModelWithoutDetailModel();

  /**
   * @return a EntityEditModel based on the department entity
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract EditModel createDepartmentEditModel();

  /**
   * @return a EntityTableModel based on the employee entity
   * @see TestDomain#T_EMP
   */
  protected abstract TableModel createEmployeeTableModel();

  /**
   * @return a EntityTableModel based on the department entity
   * @see TestDomain#T_DEPARTMENT
   */
  protected abstract TableModel createDepartmentTableModel();

  /**
   * @return a EntityModel based on the employee entity
   * @see TestDomain#T_EMP
   */
  protected abstract Model createEmployeeModel();
}