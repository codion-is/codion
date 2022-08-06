/*
 * Copyright (c) 2008 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    departmentModel.tableModel().refresh();
    EntityEditModel deptEditModel = departmentModel.editModel();
    TableModel deptTableModel = departmentModel.tableModel();
    Key operationsKey = deptEditModel.entities().primaryKey(TestDomain.T_DEPARTMENT, 40);//operations
    deptTableModel.selectByKey(singletonList(operationsKey));

    assertTrue(deptTableModel.selectionModel().isSelectionNotEmpty());
    deptEditModel.put(TestDomain.DEPARTMENT_ID, 80);
    assertFalse(deptTableModel.selectionModel().isSelectionEmpty());
    deptEditModel.update();

    assertFalse(deptTableModel.selectionModel().isSelectionEmpty());
    Entity operations = deptTableModel.selectionModel().getSelectedItem();
    assertEquals(80, operations.get(TestDomain.DEPARTMENT_ID));

    deptTableModel.setIncludeCondition(item ->
            !Objects.equals(80, item.get(TestDomain.DEPARTMENT_ID)));

    deptEditModel.setEntity(operations);
    deptEditModel.put(TestDomain.DEPARTMENT_ID, 40);
    deptEditModel.update();

    deptTableModel.filterContents();

    assertTrue(deptTableModel.filteredItems().isEmpty());
  }

  @Test
  public void testDetailModels() throws DatabaseException, ValidationException {
    //todo
  }

  @Test
  public void detailModelNotFound() {
    assertThrows(IllegalArgumentException.class, () -> departmentModel.detailModel(TestDomain.T_DEPARTMENT));
  }

  @Test
  public void clear() {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    departmentModel.tableModel().refresh();
    assertTrue(departmentModel.tableModel().getRowCount() > 0);

    Model employeeModel = departmentModel.detailModel(TestDomain.T_EMP);
    employeeModel.tableModel().refresh();
    assertTrue(employeeModel.tableModel().getRowCount() > 0);

    departmentModel.clearDetailModels();
    assertEquals(0, employeeModel.tableModel().getRowCount());

    departmentModel.clear();
    assertEquals(0, departmentModel.tableModel().getRowCount());
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
    departmentModel.tableModel().refresh();
    departmentModel.tableModel().selectionModel().setSelectedIndexes(asList(1, 2, 3));
    assertTrue(departmentModel.tableModel().selectionModel().isSelectionNotEmpty());
    assertFalse(departmentModel.editModel().isEntityNew());
    departmentModel.editModel().setDefaultValues();
    assertTrue(departmentModel.tableModel().selectionModel().isSelectionEmpty());
  }

  @Test
  public void test() throws Exception {
    assertNull(departmentModel.getMasterModel());
    assertNotNull(departmentModel.editModel());

    EventDataListener<Model> linkedListener = model -> {};
    departmentModel.addLinkedDetailModelAddedListener(linkedListener);
    departmentModel.addLinkedDetailModelRemovedListener(linkedListener);
    departmentModel.removeLinkedDetailModelAddedListener(linkedListener);
    departmentModel.removeLinkedDetailModelRemovedListener(linkedListener);
  }

  @Test
  public void detailModel() throws Exception {
    departmentModel.detailModel((Class<? extends Model>) departmentModel.detailModel(TestDomain.T_EMP).getClass());
    assertTrue(departmentModel.containsDetailModel(TestDomain.T_EMP));
    assertTrue(departmentModel.containsDetailModel(departmentModel.detailModel(TestDomain.T_EMP)));
    assertTrue(departmentModel.containsDetailModel((Class<? extends Model>) departmentModel.detailModel(TestDomain.T_EMP).getClass()));
    assertEquals(1, departmentModel.detailModels().size(), "Only one detail model should be in DepartmentModel");
    assertEquals(1, departmentModel.linkedDetailModels().size());

    departmentModel.detailModel(TestDomain.T_EMP);

    assertTrue(departmentModel.linkedDetailModels().contains(departmentModel.detailModel(TestDomain.T_EMP)));
    assertNotNull(departmentModel.detailModel(TestDomain.T_EMP));
    if (!departmentModel.containsTableModel()) {
      return;
    }

    departmentModel.tableModel().refresh();
    departmentModel.detailModel(TestDomain.T_EMP).tableModel().refresh();
    assertTrue(departmentModel.detailModel(TestDomain.T_EMP).tableModel().getRowCount() > 0);

    EntityConnection connection = departmentModel.connectionProvider().connection();
    Entity department = connection.selectSingle(TestDomain.DEPARTMENT_NAME, "SALES");

    departmentModel.tableModel().selectionModel().setSelectedItem(department);

    List<Entity> salesEmployees = connection.select(where(TestDomain.EMP_DEPARTMENT_FK).equalTo(department));
    assertFalse(salesEmployees.isEmpty());
    departmentModel.tableModel().selectionModel().setSelectedItem(department);
    List<Entity> employeesFromDetailModel =
            departmentModel.detailModel(TestDomain.T_EMP).tableModel().items();
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
    departmentModel.removeLinkedDetailModel(departmentModel.detailModel(TestDomain.T_EMP));
    assertTrue(departmentModel.linkedDetailModels().isEmpty());
    departmentModel.addLinkedDetailModel(departmentModel.detailModel(TestDomain.T_EMP));
    assertFalse(departmentModel.linkedDetailModels().isEmpty());
    assertTrue(departmentModel.linkedDetailModels().contains(departmentModel.detailModel(TestDomain.T_EMP)));
  }

  @Test
  public void filterOnMasterInsert() throws DatabaseException, ValidationException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    Model employeeModel = departmentModel.detailModel(TestDomain.T_EMP);
    employeeModel.setSearchOnMasterInsert(true);
    assertTrue(employeeModel.isSearchOnMasterInsert());
    EntityEditModel editModel = departmentModel.editModel();
    editModel.put(TestDomain.DEPARTMENT_ID, 100);
    editModel.put(TestDomain.DEPARTMENT_NAME, "Name");
    editModel.put(TestDomain.DEPARTMENT_LOCATION, "Loc");
    Entity inserted = editModel.insert();
    Collection<Entity> equalsValues = employeeModel.tableModel().tableConditionModel()
            .conditionModel(TestDomain.EMP_DEPARTMENT_FK)
            .getEqualValues();
    assertEquals(inserted, equalsValues.iterator().next());
    editModel.delete();
  }

  @Test
  public void insertDifferentTypes() throws DatabaseException, ValidationException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    Entity dept = departmentModel.connectionProvider().entities().builder(TestDomain.T_DEPARTMENT)
            .with(TestDomain.DEPARTMENT_ID, -42)
            .with(TestDomain.DEPARTMENT_NAME, "Name")
            .with(TestDomain.DEPARTMENT_LOCATION, "Loc")
            .build();

    Entity emp = connectionProvider.connection().selectSingle(TestDomain.EMP_ID, 8).clearPrimaryKey();
    emp.put(TestDomain.EMP_NAME, "NewName");

    Model model = createDepartmentModelWithoutDetailModel();
    model.editModel().insert(asList(dept, emp));
    assertTrue(model.tableModel().containsItem(dept));
    assertFalse(model.tableModel().containsItem(emp));

    model.editModel().delete(asList(dept, emp));

    assertFalse(model.tableModel().containsItem(dept));
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