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
 * Copyright (c) 2009 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.framework.model;

import is.codion.common.model.CancelException;
import is.codion.common.state.ObservableState;
import is.codion.common.state.State;
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
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.ValidationException;
import is.codion.framework.model.EntityEditModel.EntityEditor;
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
		employeeEditModel.value(Employee.HIREDATE).defaultValue().set(LocalDate::now);
		Entity jones = CONNECTION_PROVIDER.connection().selectSingle(Employee.ID.equalTo(3));//JONES, used in containsUnsavedData()
		employeeEditModel.value(Employee.MGR_FK).defaultValue().set(() -> jones);
	}

	@Test
	void postEditEvents() {
		AtomicInteger insertEvents = new AtomicInteger();
		AtomicInteger updateEvents = new AtomicInteger();
		AtomicInteger deleteEvents = new AtomicInteger();

		Consumer<Collection<Entity>> insertListener = inserted -> insertEvents.incrementAndGet();
		Consumer<Map<Entity.Key, Entity>> updateListener = udpated -> updateEvents.incrementAndGet();
		Consumer<Collection<Entity>> deleteListener = deleted -> deleteEvents.incrementAndGet();

		EntityEditEvents.insertObserver(Employee.TYPE).addWeakConsumer(insertListener);
		EntityEditEvents.updateObserver(Employee.TYPE).addWeakConsumer(updateListener);
		EntityEditEvents.deleteObserver(Employee.TYPE).addWeakConsumer(deleteListener);

		employeeEditModel.postEditEvents().set(true);

		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			Entity jones = connection.selectSingle(Employee.NAME.equalTo("JONES"));
			employeeEditModel.editor().set(jones);
			assertTrue(employeeEditModel.value(Employee.NAME).set("Noname"));
			employeeEditModel.insert();
			assertEquals(1, insertEvents.get());
			employeeEditModel.value(Employee.NAME).set("Another");
			employeeEditModel.update();
			assertEquals(1, updateEvents.get());
			assertNotNull(employeeEditModel.delete());
			assertEquals(1, deleteEvents.get());
		}
		finally {
			connection.rollbackTransaction();
		}

		EntityEditEvents.insertObserver(Employee.TYPE).removeWeakConsumer(insertListener);
		EntityEditEvents.updateObserver(Employee.TYPE).removeWeakConsumer(updateListener);
		EntityEditEvents.deleteObserver(Employee.TYPE).removeWeakConsumer(deleteListener);
	}

	@Test
	void searchModel() {
		EntitySearchModel model = employeeEditModel.searchModel(Employee.DEPARTMENT_FK);
		assertNotNull(model);
		assertSame(model, employeeEditModel.searchModel(Employee.DEPARTMENT_FK));
	}

	@Test
	void createSearchModel() {
		EntitySearchModel model = employeeEditModel.createSearchModel(Employee.DEPARTMENT_FK);
		assertNotNull(model);
		assertEquals(Department.TYPE, model.entityType());
	}

	@Test
	void refresh() {
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			Entity employee = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
			employeeEditModel.refresh();
			employeeEditModel.editor().set(employee);
			employee.put(Employee.NAME, "NOONE");
			connection.update(employee);
			employeeEditModel.refresh();
			assertEquals("NOONE", employeeEditModel.value(Employee.NAME).get());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	void entityCopy() {
		Entity employee = employeeEditModel.connection().selectSingle(
						Employee.NAME.equalTo("MARTIN"));
		employeeEditModel.editor().set(employee);
		Entity copyWithPrimaryKeyValue = employeeEditModel.editor().get();
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
	void defaultForeignKeyValue() {
		Entity employee = employeeEditModel.connection().selectSingle(
						Employee.NAME.equalTo("MARTIN"));
		EntityEditor editor = employeeEditModel.editor();
		editor.set(employee);
		//clear the department foreign key value
		Entity dept = editor.get().entity(Employee.DEPARTMENT_FK);
		employeeEditModel.value(Employee.DEPARTMENT_FK).clear();
		//set the reference key attribute value
		assertTrue(editor.isNull(Employee.DEPARTMENT_FK).get());
		assertFalse(editor.isNotNull(Employee.DEPARTMENT_FK).get());
		employeeEditModel.value(Employee.DEPARTMENT).set(dept.get(Department.ID));
		assertNull(editor.get().get(Employee.DEPARTMENT_FK));
		dept = employeeEditModel.value(Employee.DEPARTMENT_FK).get();
		assertNull(dept);
		editor.defaults();
		assertNotNull(employeeEditModel.value(Employee.DEPARTMENT_FK).get());
	}

	@Test
	void defaults() {
		employeeEditModel.value(Employee.NAME).defaultValue().set(() -> "Scott");
		EntityEditor editor = employeeEditModel.editor();
		assertTrue(editor.isNull(Employee.NAME).get());
		editor.defaults();
		assertEquals("Scott", employeeEditModel.value(Employee.NAME).get());

		employeeEditModel.value(Employee.NAME).defaultValue().set(() -> null);
		editor.defaults();
		assertTrue(editor.isNull(Employee.NAME).get());
	}

	@Test
	void clear() {
		Entity employee = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		employeeEditModel.editor().set(employee);
		employeeEditModel.editor().defaults();
		assertTrue(employeeEditModel.editor().isNotNull(Employee.DEPARTMENT_FK).get());//persists
		assertTrue(employeeEditModel.editor().isNull(Employee.NAME).get());
		employeeEditModel.editor().set(employee);
		employeeEditModel.editor().clear();
		assertTrue(employeeEditModel.editor().isNull(Employee.DEPARTMENT_FK).get());//should not persist on clear
		assertTrue(employeeEditModel.editor().isNull(Employee.NAME).get());
	}

	@Test
	void test() {
		EntityEditor editor = employeeEditModel.editor();

		ObservableState primaryKeyNullState = editor.primaryKeyNull();
		ObservableState entityExistsState = editor.exists();

		assertTrue(primaryKeyNullState.get());
		assertFalse(entityExistsState.get());

		Consumer<Object> consumer = data -> {};
		employeeEditModel.afterDelete().addConsumer(consumer);
		employeeEditModel.afterInsert().addConsumer(consumer);
		employeeEditModel.afterUpdate().addConsumer(consumer);
		employeeEditModel.beforeDelete().addConsumer(consumer);
		employeeEditModel.beforeInsert().addConsumer(consumer);
		employeeEditModel.beforeUpdate().addConsumer(consumer);
		Runnable listener = () -> {};
		employeeEditModel.afterInsertUpdateOrDelete().addListener(listener);

		assertEquals(Employee.TYPE, employeeEditModel.entityType());

		assertFalse(editor.exists().get());
		assertFalse(editor.modified().get());

		Entity employee = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		editor.set(employee);
		assertFalse(primaryKeyNullState.get());
		assertTrue(entityExistsState.get());

		assertTrue(editor.get().equalValues(employee), "Active entity is not equal to the entity just set");
		assertTrue(editor.exists().get(), "Active entity exists after an entity is set");
		assertFalse(editor.modified().get());
		editor.defaults();
		assertFalse(editor.exists().get(), "Active entity exists after defaults are set");
		assertFalse(editor.modified().get());
		assertTrue(editor.get().primaryKey().isNull(), "Active entity primary key is not null after defaults are set");

		editor.set(employee);
		assertTrue(editor.get().primaryKey().isNotNull(), "Active entity primary key is null after entity is set");

		Integer originalEmployeeId = employeeEditModel.value(Employee.ID).get();
		employeeEditModel.value(Employee.ID).clear();
		assertTrue(primaryKeyNullState.get());
		employeeEditModel.value(Employee.ID).set(originalEmployeeId);
		assertFalse(primaryKeyNullState.get());

		editor.defaults();
		assertFalse(entityExistsState.get());

   	Double originalCommission = employeeEditModel.value(Employee.COMMISSION).get();
		double commission = 1500.5;
		LocalDate originalHiredate = employeeEditModel.value(Employee.HIREDATE).get();
		LocalDate hiredate = LocalDate.now();
		String originalName = employeeEditModel.value(Employee.NAME).get();
		String name = "Mr. Mr";

		employeeEditModel.value(Employee.COMMISSION).set(commission);
		employeeEditModel.value(Employee.HIREDATE).set(hiredate);
		employeeEditModel.value(Employee.NAME).set(name);

		assertEquals(commission, employeeEditModel.value(Employee.COMMISSION).get(), "Commission does not fit");
		assertEquals(hiredate, employeeEditModel.value(Employee.HIREDATE).get(), "Hiredate does not fit");
		assertEquals(name, employeeEditModel.value(Employee.NAME).get(), "Name does not fit");

		employeeEditModel.value(Employee.COMMISSION).set(originalCommission);
		employeeEditModel.value(Employee.HIREDATE).set(originalHiredate);
		employeeEditModel.value(Employee.NAME).set(originalName);

		employeeEditModel.value(Employee.COMMISSION).set(50d);
		employeeEditModel.value(Employee.COMMISSION).clear();
		assertNull(employeeEditModel.value(Employee.COMMISSION).get());

		//test validation
		try {
			employeeEditModel.value(Employee.COMMISSION).set(50d);
			employeeEditModel.editor().validate(Employee.COMMISSION);
			fail("Validation should fail on invalid commission value");
		}
		catch (ValidationException e) {
			assertEquals(Employee.COMMISSION, e.attribute());
			assertEquals(50d, e.value());
			AttributeDefinition<?> attributeDefinition = ENTITIES.definition(Employee.TYPE).attributes().definition(e.attribute());
			assertTrue(e.getMessage().contains(attributeDefinition.toString()));
			assertTrue(e.getMessage().contains(attributeDefinition.minimumValue().toString()));
		}

		editor.defaults();
		assertTrue(editor.get().primaryKey().isNull(), "Active entity is not null after model is cleared");

		employeeEditModel.afterDelete().removeConsumer(consumer);
		employeeEditModel.afterInsert().removeConsumer(consumer);
		employeeEditModel.afterUpdate().removeConsumer(consumer);
		employeeEditModel.beforeDelete().removeConsumer(consumer);
		employeeEditModel.beforeInsert().removeConsumer(consumer);
		employeeEditModel.beforeUpdate().removeConsumer(consumer);
		employeeEditModel.afterInsertUpdateOrDelete().removeListener(listener);
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
	void insert() {
		assertTrue(employeeEditModel.insert(emptyList()).isEmpty());
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			employeeEditModel.value(Employee.COMMISSION).set(1000d);
			employeeEditModel.value(Employee.HIREDATE).set(LocalDate.now());
			employeeEditModel.value(Employee.JOB).set("CLERK");
			employeeEditModel.value(Employee.NAME).set("Björn");
			employeeEditModel.value(Employee.SALARY).set(1000d);

			Entity tmpDept = ENTITIES.builder(Department.TYPE)
							.with(Department.ID, 99)
							.with(Department.LOCATION, "Limbo")
							.with(Department.NAME, "Judgment")
							.build();

			Entity department = connection.insertSelect(tmpDept);

			employeeEditModel.value(Employee.DEPARTMENT_FK).set(department);

			employeeEditModel.afterInsert().addConsumer(insertedEntities ->
							assertEquals(department, insertedEntities.iterator().next().get(Employee.DEPARTMENT_FK)));
			employeeEditModel.insertEnabled().set(false);
			assertFalse(employeeEditModel.insertEnabled().get());
			assertThrows(IllegalStateException.class, () -> employeeEditModel.insert());
			employeeEditModel.insertEnabled().set(true);
			assertTrue(employeeEditModel.insertEnabled().get());

			employeeEditModel.insert();
			assertTrue(employeeEditModel.editor().exists().get());
			Entity entityCopy = employeeEditModel.editor().get();
			assertTrue(entityCopy.primaryKey().isNotNull());
			assertEquals(entityCopy.primaryKey(), entityCopy.originalPrimaryKey());

			employeeEditModel.value(Employee.NAME).set("Bobby");
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
	void update() {
		assertTrue(employeeEditModel.update(emptyList()).isEmpty());
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			employeeEditModel.editor().set(connection.selectSingle(Employee.NAME.equalTo("MILLER")));
			assertFalse(employeeEditModel.editor().modified().get());
			employeeEditModel.value(Employee.NAME).set("BJORN");
			assertTrue(employeeEditModel.editor().modified().get());
			List<Entity> toUpdate = singletonList(employeeEditModel.editor().get());
			Consumer<Map<Entity.Key, Entity>> consumer = updatedEntities ->
							assertEquals(toUpdate, new ArrayList<>(updatedEntities.values()));
			employeeEditModel.afterUpdate().addConsumer(consumer);
			employeeEditModel.updateEnabled().set(false);
			assertFalse(employeeEditModel.updateEnabled().get());
			assertThrows(IllegalStateException.class, () -> employeeEditModel.update());
			employeeEditModel.updateEnabled().set(true);
			assertTrue(employeeEditModel.updateEnabled().get());

			employeeEditModel.update();
			assertFalse(employeeEditModel.editor().modified().get());
			employeeEditModel.afterUpdate().removeConsumer(consumer);

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
	void delete() {
		assertTrue(employeeEditModel.delete(emptyList()).isEmpty());
		EntityConnection connection = employeeEditModel.connection();
		connection.startTransaction();
		try {
			employeeEditModel.editor().set(connection.selectSingle(Employee.NAME.equalTo("MILLER")));
			List<Entity> toDelete = singletonList(employeeEditModel.editor().get());
			employeeEditModel.afterDelete().addConsumer(deletedEntities -> assertTrue(toDelete.containsAll(deletedEntities)));
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
	void setEntity() {
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
		EntityEditor editor = employeeEditModel.editor();
		editor.set(king);
		employeeEditModel.value(Employee.MGR_FK).set(martin);
		editor.defaults();
		king.put(Employee.MGR_FK, null);
		editor.set(king);
		assertNull(employeeEditModel.value(Employee.MGR_FK).get());
		editor.defaults();
		assertEquals(LocalDate.now(), employeeEditModel.value(Employee.HIREDATE).get());
		assertFalse(editor.get().modified(Employee.HIREDATE));
		assertFalse(editor.get().modified());
	}

	@Test
	void persist() {
		Entity king = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("KING"));
		EntityEditor editor = employeeEditModel.editor();
		editor.set(king);
		assertNotNull(employeeEditModel.value(Employee.JOB).get());
		employeeEditModel.value(Employee.JOB).persist().set(true);
		editor.defaults();
		assertNotNull(employeeEditModel.value(Employee.JOB).get());
		editor.set(king);
		employeeEditModel.value(Employee.JOB).persist().set(false);
		editor.defaults();
		assertNull(employeeEditModel.value(Employee.JOB).get());
		assertThrows(IllegalArgumentException.class, () -> employeeEditModel.value(Department.ID).persist().set(true));
		assertThrows(IllegalArgumentException.class, () -> employeeEditModel.value(Department.ID).persist().get());
	}

	@Test
	void replace() {
		Entity james = employeeEditModel.connection()
						.selectSingle(Employee.NAME.equalTo("JAMES"));
		employeeEditModel.editor().set(james);
		assertTrue(employeeEditModel.editor().get().entity(Employee.MGR_FK).isNull(Employee.COMMISSION));
		Entity blake = employeeEditModel.connection()
						.selectSingle(Employee.NAME.equalTo("BLAKE"));
		blake.put(Employee.COMMISSION, 100d);
		employeeEditModel.replace(Employee.MGR_FK, singletonList(blake));
		assertEquals(100d, employeeEditModel.editor().get().entity(Employee.MGR_FK).get(Employee.COMMISSION));
	}

	@Test
	void value() {
		Value<Integer> value = employeeEditModel.value(Employee.MGR);
		assertSame(value, employeeEditModel.value(Employee.MGR));
		value.set(42);
		assertEquals(42, employeeEditModel.value(Employee.MGR).get());
		employeeEditModel.value(Employee.MGR).set(2);
		assertEquals(2, value.get());
		value.clear();
		assertFalse(employeeEditModel.value(Employee.MGR).optional().isPresent());
		assertFalse(employeeEditModel.value(Employee.MGR_FK).optional().isPresent());
		value.set(3);
		assertTrue(employeeEditModel.value(Employee.MGR).optional().isPresent());

		assertThrows(IllegalArgumentException.class, () -> employeeEditModel.value(Department.ID));
		assertThrows(IllegalArgumentException.class, () -> employeeEditModel.value(Department.ID));
	}

	@Test
	void derivedAttributes() {
		EntityEditModel editModel = new DetailEditModel(employeeEditModel.connectionProvider());

		AtomicInteger derivedCounter = new AtomicInteger();
		AtomicInteger derivedEditCounter = new AtomicInteger();

		editModel.value(Detail.INT_DERIVED).addConsumer(value -> derivedCounter.incrementAndGet());
		editModel.value(Detail.INT_DERIVED).edited().addConsumer(value -> derivedEditCounter.incrementAndGet());

		editModel.value(Detail.INT).set(1);
		assertEquals(1, derivedCounter.get());
		assertEquals(1, derivedEditCounter.get());

		editModel.value(Detail.INT).set(2);
		assertEquals(2, derivedCounter.get());
		assertEquals(2, derivedEditCounter.get());

		Entity detail = ENTITIES.builder(Detail.TYPE)
						.with(Detail.INT, 3)
						.build();
		editModel.editor().set(detail);
		assertEquals(3, derivedCounter.get());
		assertEquals(2, derivedEditCounter.get());
	}

	@Test
	void persistWritableForeignKey() {
		EntityEditModel editModel = new DetailEditModel(employeeEditModel.connectionProvider());
		assertFalse(editModel.value(Detail.MASTER_FK).persist().get());//not writable
	}

	@Test
	void foreignKeys() {
		AtomicInteger deptNoChange = new AtomicInteger();
		employeeEditModel.value(Employee.DEPARTMENT).addConsumer(value -> deptNoChange.incrementAndGet());
		AtomicInteger deptChange = new AtomicInteger();
		employeeEditModel.value(Employee.DEPARTMENT_FK).addConsumer(value -> deptChange.incrementAndGet());
		AtomicInteger deptEdit = new AtomicInteger();
		employeeEditModel.value(Employee.DEPARTMENT_FK).edited().addConsumer(value -> deptEdit.incrementAndGet());

		Entity dept = employeeEditModel.connection().selectSingle(Department.ID.equalTo(10));
		employeeEditModel.value(Employee.DEPARTMENT_FK).set(dept);

		employeeEditModel.value(Employee.DEPARTMENT).set(20);
		assertEquals(2, deptNoChange.get());
		assertEquals(2, deptChange.get());
		assertEquals(2, deptEdit.get());

		dept = employeeEditModel.connection().selectSingle(Department.ID.equalTo(20));
		employeeEditModel.value(Employee.DEPARTMENT_FK).set(dept);

		assertEquals(2, deptNoChange.get());
		assertEquals(3, deptChange.get());
		assertEquals(3, deptEdit.get());

		employeeEditModel.value(Employee.DEPARTMENT).set(30);

		assertEquals(3, deptNoChange.get());
		assertEquals(4, deptChange.get());
		assertEquals(4, deptEdit.get());

		dept = employeeEditModel.connection().selectSingle(Department.ID.equalTo(30));
		employeeEditModel.value(Employee.DEPARTMENT_FK).set(dept);

		assertEquals(3, deptNoChange.get());
		assertEquals(5, deptChange.get());
		assertEquals(5, deptEdit.get());
	}

	@Test
	void modifiedAndNullObserver() {
		employeeEditModel.value(Employee.NAME).set("NAME");
		//only modified when the entity is not new
		assertFalse(employeeEditModel.value(Employee.NAME).modified().get());
		Entity martin = employeeEditModel.connection().selectSingle(Employee.NAME.equalTo("MARTIN"));
		employeeEditModel.editor().set(martin);
		State modifiedState = State.state();
		State nullState = State.state();
		ObservableState nameModifiedObserver = employeeEditModel.value(Employee.NAME).modified();
		nameModifiedObserver.addConsumer(modifiedState::set);
		ObservableState nameIsNull = employeeEditModel.editor().isNull(Employee.NAME);
		nameIsNull.addConsumer(nullState::set);

		employeeEditModel.value(Employee.NAME).set("JOHN");
		assertTrue(nameModifiedObserver.get());
		assertTrue(modifiedState.get());
		employeeEditModel.value(Employee.NAME).clear();
		assertTrue(nameModifiedObserver.get());
		assertTrue(modifiedState.get());
		assertTrue(nameIsNull.get());
		assertTrue(nullState.get());
		assertTrue(nullState.get());
		employeeEditModel.editor().set(martin);
		assertFalse(nameModifiedObserver.get());
		assertFalse(modifiedState.get());
		assertFalse(nameIsNull.get());
		assertFalse(nullState.get());
	}

	@Test
	void modifiedUpdate() {
		EntityConnection connection = employeeEditModel.connection();
		ObservableState nameModifiedObserver = employeeEditModel.value(Employee.NAME).modified();
		Entity martin = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
		employeeEditModel.editor().set(martin);
		employeeEditModel.value(Employee.NAME).set("MARTINEZ");
		assertTrue(nameModifiedObserver.get());
		connection.startTransaction();
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

		List<Attribute<?>> changed = new ArrayList<>();
		editModel.value(Derived.INT1).addListener(() -> changed.add(Derived.INT1));
		editModel.value(Derived.INT2).addListener(() -> changed.add(Derived.INT2));
		editModel.value(Derived.INT3).addListener(() -> changed.add(Derived.INT3));
		editModel.value(Derived.INT4).addListener(() -> changed.add(Derived.INT4));

		List<Attribute<?>> edited = new ArrayList<>();
		editModel.value(Derived.INT1).edited().addListener(() -> edited.add(Derived.INT1));
		editModel.value(Derived.INT2).edited().addListener(() -> edited.add(Derived.INT2));
		editModel.value(Derived.INT3).edited().addListener(() -> edited.add(Derived.INT3));
		editModel.value(Derived.INT4).edited().addListener(() -> edited.add(Derived.INT4));

		// INT2 is derived from INT1, INT3 from INT2 etc.,
		// each adding one to the value it is derived from
		editModel.value(Derived.INT1).set(1);
		assertEquals(1, editModel.value(Derived.INT1).get());
		assertEquals(2, editModel.value(Derived.INT2).get());
		assertEquals(3, editModel.value(Derived.INT3).get());
		assertEquals(4, editModel.value(Derived.INT4).get());

		// Assert the order in which change/edit events are received
		List<Column<Integer>> expected = Arrays.asList(Derived.INT1, Derived.INT2, Derived.INT3, Derived.INT4);
		assertEquals(expected, changed);
		assertEquals(expected, edited);
	}

	@Test
	public void revert() {
		EntityEditModel editModel = new TestEntityEditModel(Employee.TYPE, CONNECTION_PROVIDER);
		EntityConnection connection = employeeEditModel.connection();
		Entity martin = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
		EntityEditor editor = editModel.editor();
		editor.set(martin);

		editModel.value(Employee.NAME).set("newname");
		assertTrue(editor.modified().get());
		editModel.value(Employee.NAME).revert();
		assertFalse(editor.modified().get());

		editModel.value(Employee.NAME).set("another");
		editModel.value(Employee.HIREDATE).set(LocalDate.now());
		assertTrue(editor.modified().get());
		editor.revert();
		assertFalse(editor.modified().get());
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
