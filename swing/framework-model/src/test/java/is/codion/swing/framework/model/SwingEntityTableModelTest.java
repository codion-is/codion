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
 * Copyright (c) 2008 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.OrderBy;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityTableConditionModel;
import is.codion.framework.model.test.AbstractEntityTableModelTest;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;
import is.codion.swing.common.model.component.table.FilteredTableColumn;

import org.junit.jupiter.api.Test;

import javax.swing.SortOrder;
import javax.swing.table.TableColumn;
import java.awt.Color;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.Optional;

import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class SwingEntityTableModelTest extends AbstractEntityTableModelTest<SwingEntityEditModel, SwingEntityTableModel> {

  @Override
  protected SwingEntityTableModel createTestTableModel() {
    SwingEntityTableModel tableModel = new SwingEntityTableModel(Detail.TYPE, connectionProvider()) {
      @Override
      protected Collection<Entity> refreshItems() {
        return testEntities;
      }
    };

    return tableModel;
  }

  @Override
  protected SwingEntityTableModel createDepartmentTableModel() {
    SwingEntityTableModel deptModel = createTableModel(Department.TYPE, testModel.connectionProvider());
    deptModel.sortModel().setSortOrder(Department.NAME, SortOrder.ASCENDING);

    return deptModel;
  }

  @Override
  protected SwingEntityTableModel createTableModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    return createTableModel(createEditModel(entityType, connectionProvider));
  }

  @Override
  protected SwingEntityTableModel createTableModel(SwingEntityEditModel editModel) {
    return new SwingEntityTableModel(editModel);
  }

  @Override
  protected SwingEntityEditModel createEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
    return new SwingEntityEditModel(entityType, connectionProvider);
  }

  @Test
  void refreshOnForeignKeyConditionValuesSet() throws DatabaseException {
    SwingEntityTableModel employeeTableModel = createTableModel(Employee.TYPE, connectionProvider());
    assertEquals(0, employeeTableModel.getRowCount());
    Entity accounting = connectionProvider().connection().selectSingle(Department.ID.equalTo(10));
    employeeTableModel.conditionModel().setEqualConditionValues(Employee.DEPARTMENT_FK, singletonList(accounting));
    employeeTableModel.refresh();
    assertEquals(7, employeeTableModel.getRowCount());
  }

  @Test
  void nullConditionModel() {
    assertThrows(NullPointerException.class, () -> new SwingEntityTableModel(Employee.TYPE, null));
  }

  @Test
  void testFiltering() {
    testModel.refresh();
    ColumnConditionModel<?, String> filterModel =
            testModel.filterModel().conditionModel(Detail.STRING);
    filterModel.setEqualValue("a");
    testModel.filterItems();
    assertEquals(4, testModel.filteredItems().size());
  }

  @Test
  void getColumnIndex() {
    assertEquals(0, testModel.columnModel().getColumnIndex(Detail.INT));
    assertEquals(1, testModel.columnModel().getColumnIndex(Detail.DOUBLE));
    assertEquals(2, testModel.columnModel().getColumnIndex(Detail.STRING));
    assertEquals(3, testModel.columnModel().getColumnIndex(Detail.DATE));
    assertEquals(4, testModel.columnModel().getColumnIndex(Detail.TIMESTAMP));
    assertEquals(5, testModel.columnModel().getColumnIndex(Detail.BOOLEAN));
    assertEquals(6, testModel.columnModel().getColumnIndex(Detail.BOOLEAN_NULLABLE));
    assertEquals(7, testModel.columnModel().getColumnIndex(Detail.MASTER_FK));
    assertEquals(8, testModel.columnModel().getColumnIndex(Detail.MASTER_NAME));
    assertEquals(9, testModel.columnModel().getColumnIndex(Detail.MASTER_CODE));
  }

  @Test
  void getValueAt() {
    testModel.refresh();
    assertEquals(1, testModel.getValueAt(0, 0));
    assertEquals(2, testModel.getValueAt(1, 0));
    assertEquals(3, testModel.getValueAt(2, 0));
    assertEquals(4, testModel.getValueAt(3, 0));
    assertEquals(5, testModel.getValueAt(4, 0));
    assertEquals("a", testModel.getValueAt(0, 2));
    assertEquals("b", testModel.getValueAt(1, 2));
    assertEquals("c", testModel.getValueAt(2, 2));
    assertEquals("d", testModel.getValueAt(3, 2));
    assertEquals("e", testModel.getValueAt(4, 2));
  }

  @Test
  void editable() {
    testModel.editable().set(true);
    assertTrue(testModel.isCellEditable(0, 0));
    assertFalse(testModel.isCellEditable(0, testModel.columnModel().getColumnIndex(Detail.INT_DERIVED)));
    testModel.editable().set(false);
  }

  @Test
  void setValueAt() {
    SwingEntityTableModel tableModel = createTableModel(Employee.TYPE, connectionProvider());
    tableModel.refresh();
    assertThrows(IllegalStateException.class, () -> tableModel.setValueAt("newname", 0, 1));
    tableModel.editable().set(true);
    tableModel.setValueAt("newname", 0, 1);
    Entity entity = tableModel.itemAt(0);
    assertEquals("newname", entity.get(Employee.NAME));
    assertThrows(RuntimeException.class, () -> tableModel.setValueAt("newname", 0, 0));
  }

  @Test
  void columnModel() {
    FilteredTableColumn<Attribute<?>> column = testModel.columnModel().column(Detail.STRING);
    assertEquals(Detail.STRING, column.getIdentifier());
  }

  @Test
  void getColumnClass() {
    assertEquals(Integer.class, testModel.getColumnClass(0));
    assertEquals(Double.class, testModel.getColumnClass(1));
    assertEquals(String.class, testModel.getColumnClass(2));
    assertEquals(LocalDate.class, testModel.getColumnClass(3));
    assertEquals(LocalDateTime.class, testModel.getColumnClass(4));
    assertEquals(Boolean.class, testModel.getColumnClass(5));
    assertEquals(Boolean.class, testModel.getColumnClass(6));
    assertEquals(Entity.class, testModel.getColumnClass(7));
  }

  @Test
  void backgroundColor() {
    SwingEntityTableModel employeeTableModel = createTableModel(Employee.TYPE, connectionProvider());
    ColumnConditionModel<Attribute<String>, String> nameConditionModel =
            employeeTableModel.conditionModel().attributeModel(Employee.NAME);
    nameConditionModel.setEqualValue("BLAKE");
    employeeTableModel.refresh();
    assertEquals(Color.GREEN, employeeTableModel.backgroundColor(0, Employee.JOB));
  }

  @Test
  void indexOf() {
    SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, testModel.connectionProvider());
    tableModel.refresh();
    tableModel.sortModel().setSortOrder(Employee.NAME, SortOrder.ASCENDING);
    assertEquals(SortOrder.ASCENDING, tableModel.sortModel().sortOrder(Employee.NAME));

    Entity.Key pk1 = connectionProvider().entities().primaryKey(Employee.TYPE, 10);//ADAMS
    assertEquals(0, tableModel.indexOf(pk1));

    Entity.Key pk2 = connectionProvider().entities().primaryKey(Employee.TYPE, -66);
    assertEquals(-1, tableModel.indexOf(pk2));
  }

  @Test
  void preferences() throws Exception {
    testModel.clearPreferences();

    SwingEntityTableModel tableModel = createTestTableModel();
    assertTrue(tableModel.columnModel().visible(Detail.STRING).get());

    tableModel.columnModel().visible(Detail.STRING).set(false);
    tableModel.columnModel().moveColumn(1, 0);//double to 0, int to 1
    TableColumn column = tableModel.columnModel().getColumn(3);
    column.setWidth(150);//timestamp
    column = tableModel.columnModel().getColumn(5);
    column.setWidth(170);//entity_ref
    ColumnConditionModel<Attribute<String>, String> conditionModel = tableModel.conditionModel().attributeModel(Detail.STRING);
    conditionModel.autoEnable().set(false);
    conditionModel.automaticWildcard().set(AutomaticWildcard.PREFIX);
    conditionModel.caseSensitive().set(false);

    tableModel.savePreferences();

    SwingEntityTableModel model = createTestTableModel();
    assertFalse(model.columnModel().visible(Detail.STRING).get());
    assertEquals(0, model.columnModel().getColumnIndex(Detail.DOUBLE));
    assertEquals(1, model.columnModel().getColumnIndex(Detail.INT));
    column = model.columnModel().getColumn(3);
    assertEquals(150, column.getPreferredWidth());
    column = model.columnModel().getColumn(5);
    assertEquals(170, column.getPreferredWidth());
    conditionModel = tableModel.conditionModel().attributeModel(Detail.STRING);
    assertFalse(conditionModel.autoEnable().get());
    assertEquals(conditionModel.automaticWildcard().get(), AutomaticWildcard.PREFIX);
    assertFalse(conditionModel.caseSensitive().get());

    model.clearPreferences();
    UserPreferences.flushUserPreferences();
  }

  @Test
  void orderQueryBySortOrder() {
    SwingEntityTableModel tableModel = createTableModel(Employee.TYPE, connectionProvider());
    OrderBy orderBy = tableModel.orderBy();
    //default order by for entity
    assertEquals(2, orderBy.orderByColumns().size());
    assertTrue(orderBy.orderByColumns().get(0).ascending());
    assertEquals(Employee.DEPARTMENT, orderBy.orderByColumns().get(0).column());
    assertTrue(orderBy.orderByColumns().get(1).ascending());
    assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());

    tableModel.sortModel().setSortOrder(Employee.NAME, SortOrder.ASCENDING);
    orderBy = tableModel.orderBy();
    //still default order by for entity
    assertEquals(2, orderBy.orderByColumns().size());
    assertTrue(orderBy.orderByColumns().get(0).ascending());
    assertEquals(Employee.DEPARTMENT, orderBy.orderByColumns().get(0).column());
    assertTrue(orderBy.orderByColumns().get(1).ascending());
    assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());

    tableModel.orderQueryBySortOrder().set(true);
    orderBy = tableModel.orderBy();
    assertEquals(1, orderBy.orderByColumns().size());
    assertTrue(orderBy.orderByColumns().get(0).ascending());
    assertEquals(Employee.NAME, orderBy.orderByColumns().get(0).column());

    tableModel.sortModel().setSortOrder(Employee.HIREDATE, SortOrder.DESCENDING);
    tableModel.sortModel().addSortOrder(Employee.NAME, SortOrder.ASCENDING);

    orderBy = tableModel.orderBy();
    assertEquals(2, orderBy.orderByColumns().size());
    assertFalse(orderBy.orderByColumns().get(0).ascending());
    assertEquals(Employee.HIREDATE, orderBy.orderByColumns().get(0).column());
    assertTrue(orderBy.orderByColumns().get(1).ascending());
    assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());

    tableModel.sortModel().clear();
    orderBy = tableModel.orderBy();
    //back to default order by for entity
    assertEquals(2, orderBy.orderByColumns().size());
    assertTrue(orderBy.orderByColumns().get(0).ascending());
    assertEquals(Employee.DEPARTMENT, orderBy.orderByColumns().get(0).column());
    assertTrue(orderBy.orderByColumns().get(1).ascending());
    assertEquals(Employee.NAME, orderBy.orderByColumns().get(1).column());
  }

  @Test
  void editEvents() throws DatabaseException, ValidationException {
    EntityConnectionProvider connectionProvider = connectionProvider();
    Entity researchDept = connectionProvider.connection().select(connectionProvider.entities().primaryKey(Department.TYPE, 20));

    SwingEntityTableModel tableModel = createTableModel(Employee.TYPE, connectionProvider);
    assertTrue(tableModel.handleEditEvents().get());
    tableModel.conditionModel().conditionModel(Employee.DEPARTMENT_FK).setEqualValue(researchDept);
    tableModel.refresh();

    tableModel.items().forEach(emp ->
            assertEquals("RESEARCH", emp.get(Employee.DEPARTMENT_FK).get(Department.NAME)));

    SwingEntityEditModel editModel = new SwingEntityEditModel(Department.TYPE, connectionProvider);
    editModel.set(researchDept);
    editModel.put(Department.NAME, "R&D");
    editModel.update();

    assertTrue(tableModel.getRowCount() > 0);

    tableModel.items().forEach(emp ->
            assertEquals("R&D", emp.get(Employee.DEPARTMENT_FK).get(Department.NAME)));

    tableModel.handleEditEvents().set(false);

    editModel.put(Department.NAME, "RESEARCH");
    editModel.update();

    tableModel.items().forEach(emp ->
            assertEquals("R&D", emp.get(Employee.DEPARTMENT_FK).get(Department.NAME)));
  }

  @Test
  void validItems() {
    SwingEntityTableModel tableModel = createTableModel(Employee.TYPE, connectionProvider());
    Entity dept = tableModel.entities().builder(Department.TYPE)
            .with(Department.ID, 1)
            .with(Department.NAME, "dept")
            .build();
    assertThrows(IllegalArgumentException.class, () -> tableModel.addItems(singletonList(dept)));
    assertThrows(IllegalArgumentException.class, () -> tableModel.addItemsAt(0, singletonList(dept)));

    assertThrows(NullPointerException.class, () -> tableModel.addItems(singletonList(null)));
    assertThrows(NullPointerException.class, () -> tableModel.addItemsAt(0, singletonList(null)));
  }

  @Test
  void conditionChanged() {
    SwingEntityTableModel tableModel = createTableModel(Employee.TYPE, connectionProvider());
    tableModel.refresh();
    ColumnConditionModel<?, String> nameConditionModel = tableModel.conditionModel().conditionModel(Employee.NAME);
    nameConditionModel.setEqualValue("JONES");
    assertTrue(tableModel.conditionChanged().get());
    tableModel.refresh();
    assertFalse(tableModel.conditionChanged().get());
    nameConditionModel.enabled().set(false);
    assertTrue(tableModel.conditionChanged().get());
    nameConditionModel.enabled().set(true);
    assertFalse(tableModel.conditionChanged().get());
  }

  @Test
  void isConditionEnabled() {
    SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, testModel.connectionProvider()) {
      @Override
      protected boolean conditionEnabled(EntityTableConditionModel<Attribute<?>> conditionModel) {
        return conditionModel.enabled(Employee.MGR_FK);
      }
    };
    tableModel.refresh();
    assertEquals(16, tableModel.getRowCount());
    tableModel.conditionRequired().set(true);
    tableModel.refresh();
    assertEquals(0, tableModel.getRowCount());
    ColumnConditionModel<?, Entity> mgrConditionModel = tableModel.conditionModel().conditionModel(Employee.MGR_FK);
    mgrConditionModel.setEqualValue(null);
    mgrConditionModel.enabled().set(true);
    tableModel.refresh();
    assertEquals(1, tableModel.getRowCount());
    mgrConditionModel.enabled().set(false);
    tableModel.refresh();
    assertEquals(0, tableModel.getRowCount());
  }

  @Test
  void columnFactory() {
    EntityDefinition definition = testModel.connectionProvider().entities().definition(Employee.TYPE);
    SwingEntityTableModel tableModel = new SwingEntityTableModel(Employee.TYPE, testModel.connectionProvider(),
            new SwingEntityColumnFactory(definition) {
              @Override
              protected Optional<FilteredTableColumn<Attribute<?>>> createColumn(AttributeDefinition<?> attributeDefinition, int modelIndex) {
                if (attributeDefinition.attribute().equals(Employee.COMMISSION)) {
                  return Optional.empty();
                }

                return super.createColumn(attributeDefinition, modelIndex);
              }
            });
    assertFalse(tableModel.columnModel().containsColumn(Employee.COMMISSION));
  }
}
