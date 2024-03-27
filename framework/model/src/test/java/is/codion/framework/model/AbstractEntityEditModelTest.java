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
 * Copyright (c) 2009 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.db.exception.DatabaseException;
import is.codion.common.model.CancelException;
import is.codion.common.state.State;
import is.codion.common.state.StateObserver;
import is.codion.common.user.User;
import is.codion.common.value.Value;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.AttributeDefinition;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Derived;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;

import static java.util.Collections.emptyList;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

public final class AbstractEntityEditModelTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private static final Entities ENTITIES = new TestDomain().entities();

  private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
          .domain(new TestDomain())
          .user(UNIT_TEST_USER)
          .build();

  private EntityEditModel employeeEditModel;

  @BeforeEach
  void setUp() {
    employeeEditModel = new TestEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
    employeeEditModel.setDefault(Employee.HIREDATE, LocalDate::now);
    try {
      Entity jones = CONNECTION_PROVIDER.connection().selectSingle(Employee.ID.equalTo(3));//JONES, used in containsUnsavedData()
      employeeEditModel.setDefault(Employee.MGR_FK, () -> jones);
    }
    catch (DatabaseException e) {
      throw new RuntimeException(e);
    }
  }

  @Test
  void editEvents() throws DatabaseException, ValidationException {
    AtomicInteger insertEvents = new AtomicInteger();
    AtomicInteger updateEvents = new AtomicInteger();
    AtomicInteger deleteEvents = new AtomicInteger();

    Consumer<Collection<Entity>> insertListener = inserted -> insertEvents.incrementAndGet();
    Consumer<Map<Entity.Key, Entity>> updateListener = udpated -> updateEvents.incrementAndGet();
    Consumer<Collection<Entity>> deleteListener = deleted -> deleteEvents.incrementAndGet();

    EntityEditEvents.addInsertListener(Employee.TYPE, insertListener);
    EntityEditEvents.addUpdateListener(Employee.TYPE, updateListener);
    EntityEditEvents.addDeleteListener(Employee.TYPE, deleteListener);

    employeeEditModel.editEvents().set(true);

    EntityConnection connection = employeeEditModel.connection();
    connection.beginTransaction();
    try {
      Entity jones = connection.selectSingle(Employee.NAME.equalTo("JONES"));
      employeeEditModel.set(jones);
      assertEquals("JONES", employeeEditModel.put(Employee.NAME, "Noname"));
      employeeEditModel.insert();
      assertEquals(1, insertEvents.get());
      employeeEditModel.put(Employee.NAME, "Another");
      employeeEditModel.update();
      assertEquals(1, updateEvents.get());
      employeeEditModel.delete();
      assertEquals(1, deleteEvents.get());
    }
    finally {
      connection.rollbackTransaction();
    }

    EntityEditEvents.removeInsertListener(Employee.TYPE, insertListener);
    EntityEditEvents.removeUpdateListener(Employee.TYPE, updateListener);
    EntityEditEvents.removeDeleteListener(Employee.TYPE, deleteListener);
  }

  @Test
  void listeners() {
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.addValueListener(Department.ID, value -> {}));
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.addEditListener(Department.ID, value -> {}));
  }

  @Test
  void foreignKeySearchModel() {
    EntitySearchModel model = employeeEditModel.foreignKeySearchModel(Employee.DEPARTMENT_FK);
    assertNotNull(model);
    assertSame(model, employeeEditModel.foreignKeySearchModel(Employee.DEPARTMENT_FK));
  }

  @Test
  void createForeignKeySearchModel() {
    EntitySearchModel model = employeeEditModel.createForeignKeySearchModel(Employee.DEPARTMENT_FK);
    assertNotNull(model);
    assertEquals(Department.TYPE, model.entityType());
  }

  @Test
  void refreshEntity() throws DatabaseException {
    EntityConnection connection = employeeEditModel.connection();
    connection.beginTransaction();
    try {
      Entity employee = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
      employeeEditModel.refreshEntity();
      employeeEditModel.set(employee);
      employee.put(Employee.NAME, "NOONE");
      connection.update(employee);
      employeeEditModel.refreshEntity();
      assertEquals("NOONE", employeeEditModel.get(Employee.NAME));
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void entityCopy() throws DatabaseException {
    Entity employee = employeeEditModel.connection().selectSingle(
            Employee.NAME.equalTo("MARTIN"));
    employeeEditModel.set(employee);
    Entity copyWithPrimaryKeyValue = employeeEditModel.entity();
    assertEquals(employee, copyWithPrimaryKeyValue);
    assertTrue(copyWithPrimaryKeyValue.primaryKey().isNotNull());
  }

  @Test
  void constructorNullEntityType() {
    assertThrows(NullPointerException.class, () -> new TestEntityEditModel(null, CONNECTION_PROVIDER));
  }

  @Test
  void constructorNullConnectionProvider() {
    assertThrows(NullPointerException.class, () -> new TestEntityEditModel(Employee.TYPE, null));
  }

  @Test
  void defaultForeignKeyValue() throws DatabaseException {
    Entity employee = employeeEditModel.connection().selectSingle(
            Employee.NAME.equalTo("MARTIN"));
    employeeEditModel.set(employee);
    //clear the department foreign key value
    Entity dept = employeeEditModel.referencedEntity(Employee.DEPARTMENT_FK);
    employeeEditModel.put(Employee.DEPARTMENT_FK, null);
    //set the reference key attribute value
    assertTrue(employeeEditModel.isNull(Employee.DEPARTMENT_FK).get());
    assertFalse(employeeEditModel.isNotNull(Employee.DEPARTMENT_FK).get());
    employeeEditModel.put(Employee.DEPARTMENT, dept.get(Department.ID));
    assertNull(employeeEditModel.entity().get(Employee.DEPARTMENT_FK));
    dept = employeeEditModel.get(Employee.DEPARTMENT_FK);
    assertNull(dept);
    employeeEditModel.setDefaults();
    assertNotNull(employeeEditModel.get(Employee.DEPARTMENT_FK));
  }

  @Test
  void defaults() {
    employeeEditModel.setDefault(Employee.NAME, () -> "Scott");
    assertTrue(employeeEditModel.isNull(Employee.NAME).get());
    employeeEditModel.setDefaults();
    assertEquals("Scott", employeeEditModel.get(Employee.NAME));

    employeeEditModel.setDefault(Employee.NAME, () -> null);
    employeeEditModel.setDefaults();
    assertTrue(employeeEditModel.isNull(Employee.NAME).get());
  }

  @Test
  void test() throws Exception {
    StateObserver primaryKeyNullState = employeeEditModel.primaryKeyNull();
    StateObserver entityExistsState = employeeEditModel.exists();

    assertTrue(primaryKeyNullState.get());
    assertFalse(entityExistsState.get());

    Consumer dataListener = data -> {};
    employeeEditModel.addAfterDeleteListener(dataListener);
    employeeEditModel.addAfterInsertListener(dataListener);
    employeeEditModel.addAfterUpdateListener(dataListener);
    employeeEditModel.addBeforeDeleteListener(dataListener);
    employeeEditModel.addBeforeInsertListener(dataListener);
    employeeEditModel.addBeforeUpdateListener(dataListener);
    Runnable listener = () -> {};
    employeeEditModel.addInsertUpdateOrDeleteListener(listener);

    assertEquals(Employee.TYPE, employeeEditModel.entityType());

    assertFalse(employeeEditModel.exists().get());
    assertFalse(employeeEditModel.modified().get());

    Entity employee = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
    employeeEditModel.set(employee);
    assertFalse(primaryKeyNullState.get());
    assertTrue(entityExistsState.get());

    assertTrue(employeeEditModel.entity().valuesEqual(employee), "Active entity is not equal to the entity just set");
    assertTrue(employeeEditModel.exists().get(), "Active entity exists after an entity is set");
    assertFalse(employeeEditModel.modified().get());
    employeeEditModel.setDefaults();
    assertFalse(employeeEditModel.exists().get(), "Active entity exists after entity is set to null");
    assertFalse(employeeEditModel.modified().get());
    assertTrue(employeeEditModel.entity().primaryKey().isNull(), "Active entity primary key is not null after entity is set to null");

    employeeEditModel.set(employee);
    assertTrue(employeeEditModel.entity().primaryKey().isNotNull(), "Active entity primary key is null after entity is set");

    Integer originalEmployeeId = employeeEditModel.get(Employee.ID);
    employeeEditModel.put(Employee.ID, null);
    assertTrue(primaryKeyNullState.get());
    employeeEditModel.put(Employee.ID, originalEmployeeId);
    assertFalse(primaryKeyNullState.get());

    employeeEditModel.setDefaults();
    assertFalse(entityExistsState.get());

    Double originalCommission = employeeEditModel.get(Employee.COMMISSION);
    final double commission = 1500.5;
    LocalDate originalHiredate = employeeEditModel.get(Employee.HIREDATE);
    LocalDate hiredate = LocalDate.now();
    String originalName = employeeEditModel.get(Employee.NAME);
    final String name = "Mr. Mr";

    employeeEditModel.put(Employee.COMMISSION, commission);
    assertTrue(employeeEditModel.modified().get());
    employeeEditModel.put(Employee.HIREDATE, hiredate);
    employeeEditModel.put(Employee.NAME, name);

    assertEquals(employeeEditModel.get(Employee.COMMISSION), commission, "Commission does not fit");
    assertEquals(employeeEditModel.get(Employee.HIREDATE), hiredate, "Hiredate does not fit");
    assertEquals(employeeEditModel.get(Employee.NAME), name, "Name does not fit");

    employeeEditModel.put(Employee.COMMISSION, originalCommission);
    assertTrue(employeeEditModel.modified().get());
    assertTrue(employeeEditModel.modified().get());
    employeeEditModel.put(Employee.HIREDATE, originalHiredate);
    assertTrue(employeeEditModel.modified().get());
    employeeEditModel.put(Employee.NAME, originalName);
    assertFalse(employeeEditModel.modified().get());

    employeeEditModel.put(Employee.COMMISSION, 50d);
    assertNotNull(employeeEditModel.remove(Employee.COMMISSION));
    assertNull(employeeEditModel.get(Employee.COMMISSION));

    //test validation
    try {
      employeeEditModel.put(Employee.COMMISSION, 50d);
      employeeEditModel.validate(Employee.COMMISSION);
      fail("Validation should fail on invalid commission value");
    }
    catch (ValidationException e) {
      assertEquals(Employee.COMMISSION, e.attribute());
      assertEquals(50d, e.value());
      AttributeDefinition<?> attributeDefinition = ENTITIES.definition(Employee.TYPE).attributes().definition(e.attribute());
      assertTrue(e.getMessage().contains(attributeDefinition.toString()));
      assertTrue(e.getMessage().contains(attributeDefinition.minimumValue().toString()));
    }

    employeeEditModel.setDefaults();
    assertTrue(employeeEditModel.entity().primaryKey().isNull(), "Active entity is not null after model is cleared");

    employeeEditModel.removeAfterDeleteListener(dataListener);
    employeeEditModel.removeAfterInsertListener(dataListener);
    employeeEditModel.removeAfterUpdateListener(dataListener);
    employeeEditModel.removeBeforeDeleteListener(dataListener);
    employeeEditModel.removeBeforeInsertListener(dataListener);
    employeeEditModel.removeBeforeUpdateListener(dataListener);
    employeeEditModel.removeInsertUpdateOrDeleteListener(listener);
  }

  @Test
  void insertReadOnly() throws CancelException {
    employeeEditModel.readOnly().set(true);
    assertThrows(IllegalStateException.class, () -> employeeEditModel.insert());
  }

  @Test
  void updateReadOnly() throws CancelException {
    employeeEditModel.readOnly().set(true);
    assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
  }

  @Test
  void deleteReadOnly() throws CancelException {
    employeeEditModel.readOnly().set(true);
    assertThrows(IllegalStateException.class, () -> employeeEditModel.delete());
  }

  @Test
  void insert() throws Exception {
    assertTrue(employeeEditModel.insert(emptyList()).isEmpty());
    EntityConnection connection = employeeEditModel.connection();
    connection.beginTransaction();
    try {
      employeeEditModel.put(Employee.COMMISSION, 1000d);
      employeeEditModel.put(Employee.HIREDATE, LocalDate.now());
      employeeEditModel.put(Employee.JOB, "CLERK");
      employeeEditModel.put(Employee.NAME, "Björn");
      employeeEditModel.put(Employee.SALARY, 1000d);

      Entity tmpDept = ENTITIES.builder(Department.TYPE)
              .with(Department.ID, 99)
              .with(Department.LOCATION, "Limbo")
              .with(Department.NAME, "Judgment")
              .build();

      Entity department = connection.insertSelect(tmpDept);

      employeeEditModel.put(Employee.DEPARTMENT_FK, department);

      employeeEditModel.addAfterInsertListener(insertedEntities ->
              assertEquals(department, insertedEntities.iterator().next().get(Employee.DEPARTMENT_FK)));
      employeeEditModel.insertEnabled().set(false);
      assertFalse(employeeEditModel.insertEnabled().get());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.insert());
      employeeEditModel.insertEnabled().set(true);
      assertTrue(employeeEditModel.insertEnabled().get());

      employeeEditModel.insert();
      assertTrue(employeeEditModel.exists().get());
      Entity entityCopy = employeeEditModel.entity();
      assertTrue(entityCopy.primaryKey().isNotNull());
      assertEquals(entityCopy.primaryKey(), entityCopy.originalPrimaryKey());

      employeeEditModel.put(Employee.NAME, "Bobby");
      try {
        employeeEditModel.insert();
      }
      catch (Exception e) {
        fail("Should be able to insert again");
      }
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void update() throws Exception {
    assertTrue(employeeEditModel.update(emptyList()).isEmpty());
    EntityConnection connection = employeeEditModel.connection();
    connection.beginTransaction();
    try {
      employeeEditModel.set(connection.selectSingle(Employee.NAME.equalTo("MILLER")));
      employeeEditModel.put(Employee.NAME, "BJORN");
      List<Entity> toUpdate = singletonList(employeeEditModel.entity());
      Consumer<Map<Entity.Key, Entity>> listener = updatedEntities ->
              assertEquals(toUpdate, new ArrayList<>(updatedEntities.values()));
      employeeEditModel.addAfterUpdateListener(listener);
      employeeEditModel.updateEnabled().set(false);
      assertFalse(employeeEditModel.updateEnabled().get());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
      employeeEditModel.updateEnabled().set(true);
      assertTrue(employeeEditModel.updateEnabled().get());

      employeeEditModel.update();
      assertFalse(employeeEditModel.modified().get());
      employeeEditModel.removeAfterUpdateListener(listener);

      employeeEditModel.updateMultipleEnabled().set(false);

      Entity emp1 = connection.selectSingle(Employee.NAME.equalTo("BLAKE"));
      emp1.put(Employee.COMMISSION, 100d);
      Entity emp2 = connection.selectSingle(Employee.NAME.equalTo("JONES"));
      emp2.put(Employee.COMMISSION, 100d);
      assertThrows(IllegalStateException.class, () -> employeeEditModel.update(Arrays.asList(emp1, emp2)));
    }
    finally {
      connection.rollbackTransaction();
      employeeEditModel.updateMultipleEnabled().set(true);
    }
  }

  @Test
  void delete() throws Exception {
    employeeEditModel.delete(emptyList());
    EntityConnection connection = employeeEditModel.connection();
    connection.beginTransaction();
    try {
      employeeEditModel.set(connection.selectSingle(Employee.NAME.equalTo("MILLER")));
      List<Entity> toDelete = singletonList(employeeEditModel.entity());
      employeeEditModel.addAfterDeleteListener(deletedEntities -> assertTrue(toDelete.containsAll(deletedEntities)));
      employeeEditModel.deleteEnabled().set(false);
      assertFalse(employeeEditModel.deleteEnabled().get());
      assertThrows(IllegalStateException.class, () -> employeeEditModel.delete());
      employeeEditModel.deleteEnabled().set(true);
      assertTrue(employeeEditModel.deleteEnabled().get());

      employeeEditModel.delete();
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  void setEntity() throws Exception {
    Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
    Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
    employeeEditModel.set(king);
    employeeEditModel.put(Employee.MGR_FK, martin);
    employeeEditModel.setDefaults();
    king.put(Employee.MGR_FK, null);
    employeeEditModel.set(king);
    assertNull(employeeEditModel.get(Employee.MGR_FK));
    employeeEditModel.setDefaults();
    assertEquals(LocalDate.now(), employeeEditModel.get(Employee.HIREDATE));
    assertFalse(employeeEditModel.entity().modified(Employee.HIREDATE));
    assertFalse(employeeEditModel.entity().modified());
  }

  @Test
  void persist() throws Exception {
    Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
    employeeEditModel.set(king);
    assertNotNull(employeeEditModel.get(Employee.JOB));
    employeeEditModel.persist(Employee.JOB).set(true);
    employeeEditModel.setDefaults();
    assertNotNull(employeeEditModel.get(Employee.JOB));
    employeeEditModel.set(king);
    employeeEditModel.persist(Employee.JOB).set(false);
    employeeEditModel.setDefaults();
    assertNull(employeeEditModel.get(Employee.JOB));
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.persist(Department.ID).set(true));
    assertThrows(IllegalArgumentException.class, () -> employeeEditModel.persist(Department.ID).get());
  }

  @Test
  void containsUnsavedData() throws DatabaseException {
    employeeEditModel.overwriteWarning().set(true);
    employeeEditModel.persist(Employee.DEPARTMENT_FK).set(false);

    Consumer<State> alwaysConfirmListener = data -> data.set(true);
    Consumer<State> alwaysDenyListener = data -> data.set(false);

    employeeEditModel.addConfirmOverwriteListener(alwaysConfirmListener);
    Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
    Entity adams = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("ADAMS"));
    employeeEditModel.set(king);
    employeeEditModel.put(Employee.NAME, "New name");
    employeeEditModel.set(adams);
    assertEquals(adams, employeeEditModel.entity());

    employeeEditModel.removeConfirmOverwriteListener(alwaysConfirmListener);
    employeeEditModel.setDefaults();
    employeeEditModel.addConfirmOverwriteListener(alwaysDenyListener);

    employeeEditModel.set(adams);
    employeeEditModel.put(Employee.NAME, "A name");
    employeeEditModel.set(king);
    assertEquals("A name", employeeEditModel.get(Employee.NAME));

    employeeEditModel.removeConfirmOverwriteListener(alwaysDenyListener);
    employeeEditModel.setDefaults();
    employeeEditModel.addConfirmOverwriteListener(alwaysDenyListener);

    employeeEditModel.set(adams);
    employeeEditModel.put(Employee.DEPARTMENT_FK, king.get(Employee.DEPARTMENT_FK));
    employeeEditModel.set(adams);
    assertEquals(king.get(Employee.DEPARTMENT_FK), employeeEditModel.get(Employee.DEPARTMENT_FK));

    employeeEditModel.overwriteWarning().set(false);

    employeeEditModel.set(adams);
    employeeEditModel.put(Employee.HIREDATE, LocalDate.now());
    assertTrue(employeeEditModel.exists().get() && employeeEditModel.modified().get());

    employeeEditModel.put(Employee.HIREDATE, adams.get(Employee.HIREDATE));
    assertFalse(employeeEditModel.exists().get() && employeeEditModel.modified().get());

    employeeEditModel.persist(Employee.MGR_FK).set(false);

    employeeEditModel.put(Employee.MGR_FK, null);//default value JONES
    assertTrue(employeeEditModel.exists().get() && employeeEditModel.modified().get());
  }

  @Test
  void replace() throws DatabaseException {
    Entity james = employeeEditModel.connection()
            .selectSingle(Employee.NAME.equalTo("JAMES"));
    employeeEditModel.set(james);
    Entity blake = employeeEditModel.connection()
            .selectSingle(Employee.NAME.equalTo("BLAKE"));
    assertNotSame(employeeEditModel.referencedEntity(Employee.MGR_FK), blake);
    employeeEditModel.replace(Employee.MGR_FK, singletonList(blake));
    assertSame(employeeEditModel.referencedEntity(Employee.MGR_FK), blake);
  }

  @Test
  void value() {
    Value<Integer> value = employeeEditModel.value(Employee.MGR);
    assertSame(value, employeeEditModel.value(Employee.MGR));
    value.set(42);
    assertEquals(42, employeeEditModel.get(Employee.MGR));
    employeeEditModel.put(Employee.MGR, 2);
    assertEquals(2, value.get());
    value.set(null);
    assertFalse(employeeEditModel.optional(Employee.MGR).isPresent());
    assertFalse(employeeEditModel.optional(Employee.MGR_FK).isPresent());
    value.set(3);
    assertTrue(employeeEditModel.optional(Employee.MGR).isPresent());
  }

  @Test
  void derivedAttributes() {
    EntityEditModel editModel = new DetailEditModel(employeeEditModel.connectionProvider());

    AtomicInteger derivedCounter = new AtomicInteger();
    AtomicInteger derivedEditCounter = new AtomicInteger();

    editModel.addValueListener(Detail.INT_DERIVED, value -> derivedCounter.incrementAndGet());
    editModel.addEditListener(Detail.INT_DERIVED, value -> derivedEditCounter.incrementAndGet());

    editModel.put(Detail.INT, 1);
    assertEquals(1, derivedCounter.get());
    assertEquals(1, derivedEditCounter.get());

    editModel.put(Detail.INT, 2);
    assertEquals(2, derivedCounter.get());
    assertEquals(2, derivedEditCounter.get());

    Entity detail = ENTITIES.builder(Detail.TYPE)
            .with(Detail.INT, 3)
            .build();
    editModel.set(detail);
    assertEquals(3, derivedCounter.get());
    assertEquals(2, derivedEditCounter.get());
  }

  @Test
  void persistWritableForeignKey() {
    EntityEditModel editModel = new DetailEditModel(employeeEditModel.connectionProvider());
    assertFalse(editModel.persist(Detail.MASTER_FK).get());//not writable
  }

  @Test
  void foreignKeys() throws DatabaseException {
    AtomicInteger deptNoChange = new AtomicInteger();
    employeeEditModel.addValueListener(Employee.DEPARTMENT, value -> deptNoChange.incrementAndGet());
    AtomicInteger deptChange = new AtomicInteger();
    employeeEditModel.addValueListener(Employee.DEPARTMENT_FK, value -> deptChange.incrementAndGet());
    AtomicInteger deptEdit = new AtomicInteger();
    employeeEditModel.addEditListener(Employee.DEPARTMENT_FK, value -> deptEdit.incrementAndGet());

    Entity dept = employeeEditModel.connection().selectSingle(Department.ID.equalTo(10));
    employeeEditModel.put(Employee.DEPARTMENT_FK, dept);

    employeeEditModel.put(Employee.DEPARTMENT, 20);
    assertEquals(2, deptNoChange.get());
    assertEquals(2, deptChange.get());
    assertEquals(2, deptEdit.get());

    dept = employeeEditModel.connection().selectSingle(Department.ID.equalTo(20));
    employeeEditModel.put(Employee.DEPARTMENT_FK, dept);

    assertEquals(2, deptNoChange.get());
    assertEquals(3, deptChange.get());
    assertEquals(3, deptEdit.get());

    employeeEditModel.put(Employee.DEPARTMENT, 30);

    assertEquals(3, deptNoChange.get());
    assertEquals(4, deptChange.get());
    assertEquals(4, deptEdit.get());

    dept = employeeEditModel.connection().selectSingle(Department.ID.equalTo(30));
    employeeEditModel.put(Employee.DEPARTMENT_FK, dept);

    assertEquals(3, deptNoChange.get());
    assertEquals(5, deptChange.get());
    assertEquals(5, deptEdit.get());
  }

  @Test
  void modifiedAndNullObserver() throws DatabaseException {
    employeeEditModel.put(Employee.NAME, "NAME");
    //only modified when the entity is not new
    assertFalse(employeeEditModel.modified(Employee.NAME).get());
    Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
    employeeEditModel.set(martin);
    State modifiedState = State.state();
    State nullState = State.state();
    StateObserver nameModifiedObserver = employeeEditModel.modified(Employee.NAME);
    nameModifiedObserver.addDataListener(modifiedState::set);
    StateObserver nameIsNull = employeeEditModel.isNull(Employee.NAME);
    nameIsNull.addDataListener(nullState::set);

    employeeEditModel.put(Employee.NAME, "JOHN");
    assertTrue(nameModifiedObserver.get());
    assertTrue(modifiedState.get());
    employeeEditModel.put(Employee.NAME, null);
    assertTrue(nameModifiedObserver.get());
    assertTrue(modifiedState.get());
    assertTrue(nameIsNull.get());
    assertTrue(nullState.get());
    assertTrue(nullState.get());
    employeeEditModel.set(martin);
    assertFalse(nameModifiedObserver.get());
    assertFalse(modifiedState.get());
    assertFalse(nameIsNull.get());
    assertFalse(nullState.get());
  }

  @Test
  void modifiedUpdate() throws DatabaseException, ValidationException {
    EntityConnection connection = employeeEditModel.connection();
    StateObserver nameModifiedObserver = employeeEditModel.modified(Employee.NAME);
    Entity martin = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
    employeeEditModel.set(martin);
    employeeEditModel.put(Employee.NAME, "MARTINEZ");
    assertTrue(nameModifiedObserver.get());
    connection.beginTransaction();
    try {
      employeeEditModel.update();
      assertFalse(nameModifiedObserver.get());
    }
    finally {
      connection.rollbackTransaction();
    }
  }

  @Test
  public void derivedValues() {
    EntityEditModel editModel = new TestEntityEditModel(Derived.TYPE, CONNECTION_PROVIDER);
    assertNull(editModel.get(Derived.INT));
    assertNull(editModel.get(Derived.INT2));
    assertNull(editModel.get(Derived.INT3));

    editModel.put(Derived.INT, 1);
    assertEquals(1, editModel.get(Derived.INT));
    assertEquals(2, editModel.get(Derived.INT2));
    assertEquals(3, editModel.get(Derived.INT3));

    Map<Attribute<?>, Object> editedValues = new LinkedHashMap<>();
    editModel.addEditListener(Derived.INT2, value -> editedValues.put(Derived.INT2, value));
    editModel.addEditListener(Derived.INT3, value -> editedValues.put(Derived.INT3, value));
    editModel.addEditListener(Derived.INT4, value -> editedValues.put(Derived.INT4, value));

    Map<Attribute<?>, Object> changedValues = new LinkedHashMap<>();
    editModel.addValueListener(Derived.INT2, value -> changedValues.put(Derived.INT2, value));
    editModel.addValueListener(Derived.INT3, value -> changedValues.put(Derived.INT3, value));
    editModel.addValueListener(Derived.INT4, value -> changedValues.put(Derived.INT4, value));

    editModel.put(Derived.INT, 2);
    assertTrue(editedValues.containsKey(Derived.INT2));
    assertEquals(3, editedValues.get(Derived.INT2));
    assertTrue(changedValues.containsKey(Derived.INT2));
    assertEquals(3, changedValues.get(Derived.INT2));
    assertTrue(editedValues.containsKey(Derived.INT3));
    assertEquals(4, editedValues.get(Derived.INT3));
    assertTrue(changedValues.containsKey(Derived.INT3));
    assertEquals(4, changedValues.get(Derived.INT3));
    assertTrue(editedValues.containsKey(Derived.INT4));
    assertEquals(5, editedValues.get(Derived.INT4));
    assertTrue(changedValues.containsKey(Derived.INT4));
    assertEquals(5, changedValues.get(Derived.INT4));

    List<Attribute<?>> attributes = new ArrayList<>(editedValues.keySet());
    assertEquals(Derived.INT2, attributes.get(0));
    assertEquals(Derived.INT3, attributes.get(1));
    assertEquals(Derived.INT4, attributes.get(2));

    attributes = new ArrayList<>(changedValues.keySet());
    assertEquals(Derived.INT2, attributes.get(0));
    assertEquals(Derived.INT3, attributes.get(1));
    assertEquals(Derived.INT4, attributes.get(2));
  }

  private static final class TestEntityEditModel extends AbstractEntityEditModel {

    private TestEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
      super(entityType, connectionProvider);
    }

    @Override
    public void add(ForeignKey foreignKey, Collection<Entity> entities) {}

    @Override
    public void remove(ForeignKey foreignKey, Collection<Entity> entities) {}
  }

  private static final class DetailEditModel extends AbstractEntityEditModel {

    private DetailEditModel(EntityConnectionProvider connectionProvider) {
      super(Detail.TYPE, connectionProvider);
    }

    @Override
    public void add(ForeignKey foreignKey, Collection<Entity> entities) {}

    @Override
    public void remove(ForeignKey foreignKey, Collection<Entity> entities) {}
  }
}
