/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2008 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model.test;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.AbstractEntityEditModel;
import is.codion.framework.model.DefaultEntityModel;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.ForeignKeyDetailModelLink;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.Collection;
import java.util.List;
import java.util.Objects;

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
        EditModel extends AbstractEntityEditModel, TableModel extends EntityTableModel<EditModel>> {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  protected static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .user(UNIT_TEST_USER)
          .domain(new TestDomain())
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
    Entity.Key operationsKey = deptEditModel.entities().primaryKey(Department.TYPE, 40);//operations
    deptTableModel.select(singletonList(operationsKey));

    assertTrue(deptTableModel.selectionModel().selectionNotEmpty().get());
    deptEditModel.put(Department.ID, 80);
    assertFalse(deptTableModel.selectionModel().selectionEmpty().get());
    deptEditModel.update();

    assertFalse(deptTableModel.selectionModel().selectionEmpty().get());
    Entity operations = deptTableModel.selectionModel().getSelectedItem();
    assertEquals(80, operations.get(Department.ID));

    deptTableModel.includeCondition().set(item ->
            !Objects.equals(80, item.get(Department.ID)));

    deptEditModel.set(operations);
    deptEditModel.put(Department.ID, 40);
    deptEditModel.update();

    deptTableModel.filterItems();

    assertTrue(deptTableModel.filteredItems().isEmpty());
  }

  @Test
  public void testDetailModels() throws DatabaseException, ValidationException {
    //todo
  }

  @Test
  public void detailModelNotFound() {
    assertThrows(IllegalArgumentException.class, () -> departmentModel.detailModel(Department.TYPE));
  }

  @Test
  public void clear() {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    departmentModel.tableModel().refresh();
    assertTrue(departmentModel.tableModel().getRowCount() > 0);

    Model employeeModel = departmentModel.detailModel(Employee.TYPE);
    employeeModel.tableModel().refresh();
    assertTrue(employeeModel.tableModel().getRowCount() > 0);

    departmentModel.detailModels().forEach(detailModel -> detailModel.tableModel().clear());
    assertEquals(0, employeeModel.tableModel().getRowCount());

    departmentModel.tableModel().clear();
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
    assertTrue(departmentModel.tableModel().selectionModel().selectionNotEmpty().get());
    assertTrue(departmentModel.editModel().exists().get());
    departmentModel.editModel().setDefaults();
    assertTrue(departmentModel.tableModel().selectionModel().selectionEmpty().get());
  }

  @Test
  public void test() throws Exception {
    assertNotNull(departmentModel.editModel());
  }

  @Test
  public void detailModel() throws Exception {
    departmentModel.detailModel((Class<? extends Model>) departmentModel.detailModel(Employee.TYPE).getClass());
    assertTrue(departmentModel.containsDetailModel(Employee.TYPE));
    Model detailModel = departmentModel.detailModel(Employee.TYPE);
    assertTrue(departmentModel.containsDetailModel(detailModel));
    assertTrue(departmentModel.containsDetailModel((Class<? extends Model>) departmentModel.detailModel(Employee.TYPE).getClass()));
    assertEquals(1, departmentModel.detailModels().size(), "Only one detail model should be in DepartmentModel");
    assertEquals(1, departmentModel.activeDetailModels().size());

    departmentModel.detailModel(Employee.TYPE);

    assertTrue(departmentModel.activeDetailModels().contains(departmentModel.detailModel(Employee.TYPE)));
    assertNotNull(departmentModel.detailModel(Employee.TYPE));
    if (!departmentModel.containsTableModel()) {
      return;
    }

    departmentModel.tableModel().refresh();
    departmentModel.detailModel(Employee.TYPE).tableModel().refresh();
    assertTrue(departmentModel.detailModel(Employee.TYPE).tableModel().getRowCount() > 0);

    EntityConnection connection = departmentModel.connectionProvider().connection();
    Entity department = connection.selectSingle(Department.NAME.equalTo("SALES"));

    departmentModel.tableModel().selectionModel().setSelectedItem(department);

    List<Entity> salesEmployees = connection.select(Employee.DEPARTMENT_FK.equalTo(department));
    assertFalse(salesEmployees.isEmpty());
    departmentModel.tableModel().selectionModel().setSelectedItem(department);
    Collection<Entity> employeesFromDetailModel =
            departmentModel.detailModel(Employee.TYPE).tableModel().items();
    assertTrue(salesEmployees.containsAll(employeesFromDetailModel), "Filtered list should contain all employees for department");
  }

  @Test
  public void addSameDetailModelTwice() {
    Model model = createDepartmentModelWithoutDetailModel();
    Model employeeModel = createEmployeeModel();
    assertThrows(IllegalArgumentException.class, () -> model.addDetailModels(employeeModel, employeeModel));
  }

  @Test
  public void addModelAsItsOwnDetailModel() {
    Model model = createDepartmentModelWithoutDetailModel();
    assertThrows(IllegalArgumentException.class, () -> model.addDetailModel(model));
  }

  @Test
  public void activateDeactivateDetailModel() {
    departmentModel.detailModelLink(departmentModel.detailModel(Employee.TYPE)).active().set(false);
    assertTrue(departmentModel.activeDetailModels().isEmpty());
    departmentModel.detailModelLink(departmentModel.detailModel(Employee.TYPE)).active().set(true);
    assertFalse(departmentModel.activeDetailModels().isEmpty());
    assertTrue(departmentModel.activeDetailModels().contains(departmentModel.detailModel(Employee.TYPE)));
  }

  @Test
  public void searchByInsertedEntity() throws DatabaseException, ValidationException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    Model employeeModel = departmentModel.detailModel(Employee.TYPE);
    ForeignKeyDetailModelLink<Model, EditModel, TableModel> link = departmentModel.detailModelLink(employeeModel);
    link.searchByInsertedEntity().set(true);
    assertTrue(link.searchByInsertedEntity().get());
    EntityEditModel editModel = departmentModel.editModel();
    editModel.put(Department.ID, 100);
    editModel.put(Department.NAME, "Name");
    editModel.put(Department.LOCATION, "Loc");
    Entity inserted = editModel.insert();
    Collection<Entity> equalsValues = employeeModel.tableModel().conditionModel()
            .attributeModel(Employee.DEPARTMENT_FK)
            .getEqualValues();
    assertEquals(inserted, equalsValues.iterator().next());
    editModel.delete();
  }

  @Test
  public void clearForeignKeyOnEmptySelection() throws DatabaseException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    Model employeeModel = departmentModel.detailModel(Employee.TYPE);
    EditModel employeeEditModel = employeeModel.editModel();

    ForeignKeyDetailModelLink<Model, EditModel, TableModel> link = departmentModel.detailModelLink(employeeModel);
    link.clearForeignKeyOnEmptySelection().set(false);

    Entity dept = employeeModel.connectionProvider().connection().selectSingle(Department.ID.equalTo(10));

    departmentModel.tableModel().refresh();
    departmentModel.tableModel().selectionModel().setSelectedItem(dept);
    assertEquals(dept, employeeEditModel.get(Employee.DEPARTMENT_FK));

    departmentModel.tableModel().selectionModel().clearSelection();
    assertEquals(dept, employeeEditModel.get(Employee.DEPARTMENT_FK));

    link.clearForeignKeyOnEmptySelection().set(true);

    departmentModel.tableModel().selectionModel().setSelectedItem(dept);
    assertEquals(dept, employeeEditModel.get(Employee.DEPARTMENT_FK));

    departmentModel.tableModel().selectionModel().clearSelection();
    assertTrue(employeeEditModel.isNull(Employee.DEPARTMENT_FK).get());

    link.clearForeignKeyOnEmptySelection().set(false);

    departmentModel.tableModel().selectionModel().setSelectedItem(dept);
    assertEquals(dept, employeeEditModel.get(Employee.DEPARTMENT_FK));
  }

  @Test
  public void refreshOnSelection() throws DatabaseException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    Model employeeModel = departmentModel.detailModel(Employee.TYPE);
    TableModel employeeTableModel = employeeModel.tableModel();

    ForeignKeyDetailModelLink<Model, EditModel, TableModel> link = departmentModel.detailModelLink(employeeModel);
    link.refreshOnSelection().set(false);

    Entity dept = employeeModel.connectionProvider().connection().selectSingle(Department.ID.equalTo(10));

    departmentModel.tableModel().refresh();
    departmentModel.tableModel().selectionModel().setSelectedItem(dept);
    assertEquals(0, employeeTableModel.getRowCount());

    link.refreshOnSelection().set(true);
    departmentModel.tableModel().selectionModel().setSelectedItem(dept);
    assertNotEquals(0, employeeTableModel.getRowCount());
  }

  @Test
  public void insertDifferentTypes() throws DatabaseException, ValidationException {
    if (!departmentModel.containsTableModel()) {
      return;
    }
    Entity dept = departmentModel.connectionProvider().entities().builder(Department.TYPE)
            .with(Department.ID, -42)
            .with(Department.NAME, "Name")
            .with(Department.LOCATION, "Loc")
            .build();

    Entity emp = connectionProvider.connection().selectSingle(Employee.ID.equalTo(8)).clearPrimaryKey();
    emp.put(Employee.NAME, "NewName");

    Model model = createDepartmentModelWithoutDetailModel();
    model.editModel().insert(asList(dept, emp));
    assertTrue(model.tableModel().containsItem(dept));
    assertFalse(model.tableModel().containsItem(emp));

    model.editModel().delete(asList(dept, emp));

    assertFalse(model.tableModel().containsItem(dept));
  }

  protected final EntityConnectionProvider connectionProvider() {
    return connectionProvider;
  }

  /**
   * @return a EntityModel based on the department entity
   * @see Department#TYPE
   */
  protected abstract Model createDepartmentModel();

  /**
   * @return a EntityModel based on the department entity, without detail models
   * @see Department#TYPE
   */
  protected abstract Model createDepartmentModelWithoutDetailModel();

  /**
   * @return a EntityModel based on the employee entity
   * @see Employee#TYPE
   */
  protected abstract Model createEmployeeModel();
}