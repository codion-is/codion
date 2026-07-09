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
 * Copyright (c) 2008 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.framework.model.test;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.model.filter.SortOrder;
import is.codion.common.model.selection.MultiSelection;
import is.codion.common.utilities.Operator;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.EntityEditModel;
import is.codion.framework.model.EntityEditor;
import is.codion.framework.model.EntityQueryModel;
import is.codion.framework.model.EntityTableModel;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Detail;
import is.codion.framework.model.test.TestDomain.Employee;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import static java.util.Arrays.asList;
import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.junit.jupiter.api.Assertions.*;

/**
 * A base class for testing {@link EntityTableModel} subclasses.
 * @param <E> the {@link EntityEditModel} type
 * @param <T> the {@link EntityTableModel} type
 * @param <R> the {@link EntityEditor} type
 */
public abstract class AbstractEntityTableModelTest<E extends EntityEditModel<R>,
				T extends EntityTableModel<E, R>, R extends EntityEditor<R>> {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.user(UNIT_TEST_USER)
					.domain(new TestDomain())
					.build();

	private static final String JONES = "JONES";
	private static final String SYNCED = "synced";
	private static final String REPLACED = "replaced";

	private final EntityConnectionProvider connectionProvider;

	protected final List<Entity> testEntities = initTestEntities(CONNECTION_PROVIDER.entities());

	protected final T testModel;

	protected AbstractEntityTableModelTest() {
		connectionProvider = CONNECTION_PROVIDER;
		testModel = createTestTableModel();
	}

	@Test
	public void select() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider);
		tableModel.items().refresh();

		List<Entity.Key> keys = tableModel.entities().primaryKeys(Employee.TYPE, 1, 2);
		Entity.Key pk1 = keys.get(0);
		Entity.Key pk2 = keys.get(1);

		tableModel.select(singletonList(pk1));
		Entity selectedPK1 = tableModel.selection().item().get();
		assertEquals(pk1, selectedPK1.primaryKey());
		assertEquals(1, tableModel.selection().count());

		tableModel.select(singletonList(pk2));
		Entity selectedPK2 = tableModel.selection().item().get();
		assertEquals(pk2, selectedPK2.primaryKey());
		assertEquals(1, tableModel.selection().count());

		tableModel.select(keys);
		List<Entity> selectedItems = tableModel.selection().items().get();
		for (Entity selected : selectedItems) {
			assertTrue(keys.contains(selected.primaryKey()));
		}
		assertEquals(2, tableModel.selection().count());
	}

	@Test
	public void selectedEntitiesIterator() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider);
		tableModel.items().refresh();

		tableModel.selection().indexes().set(asList(0, 3, 5));
		Iterator<Entity> iterator = tableModel.selection().items().get().iterator();
		assertEquals(tableModel.items().included().get().get(0), iterator.next());
		assertEquals(tableModel.items().included().get().get(3), iterator.next());
		assertEquals(tableModel.items().included().get().get(5), iterator.next());
	}

	@Test
	public void onInsertPreservesSelection() throws EntityValidationException {
		T deptModel = createDepartmentTableModel();
		deptModel.items().refresh();
		Entities entities = deptModel.entities();

		deptModel.selection().index().set(1);
		Entity selected = deptModel.selection().item().get();
		assertNotNull(selected);

		deptModel.onInsert().set(EntityTableModel.OnInsert.PREPEND);
		Entity dept = entities.entity(Department.TYPE)
						.with(Department.ID, -40)
						.with(Department.LOCATION, "Nowhere4")
						.with(Department.NAME, "AAAAA")
						.build();
		deptModel.editor().insert(singletonList(dept));

		//the inserted rows do not invalidate the selection, which is restored by item since
		//the indexed insert shifts the rows at and below the insertion point
		assertEquals(selected, deptModel.selection().item().get());
		//and since the selection never actually changed, the editor was not defaulted via the
		//selection sync, leaving the clear-after-insert decision to the UI
		assertEquals(selected.primaryKey(), deptModel.editor().entity().get().primaryKey());

		deptModel.editor().delete(singletonList(dept));
	}

	@Test
	public void onInsertDoesNotSyncEditorFromSelection() throws EntityValidationException {
		T deptModel = createDepartmentTableModel();
		deptModel.items().refresh();
		Entities entities = deptModel.entities();

		deptModel.selection().index().set(1);
		Entity selected = deptModel.selection().item().get();
		Entity other = deptModel.items().included().get(2);
		assertNotEquals(selected, other);

		//the UI insert flow leaves the inserted entity in the editor, which is not the selected row
		deptModel.editor().entity().set(other);
		assertEquals(other.primaryKey(), deptModel.editor().entity().get().primaryKey());

		deptModel.onInsert().set(EntityTableModel.OnInsert.PREPEND);
		Entity dept = entities.entity(Department.TYPE)
						.with(Department.ID, -50)
						.with(Department.LOCATION, "Nowhere5")
						.with(Department.NAME, "AAAAB")
						.build();
		deptModel.editor().insert(singletonList(dept));

		//preserving the selection re-notifies it, but the selection did not actually change,
		//so the editor must not be resynced from it, which would discard the inserted entity
		assertEquals(other.primaryKey(), deptModel.editor().entity().get().primaryKey());
		assertEquals(selected, deptModel.selection().item().get());

		deptModel.editor().delete(singletonList(dept));
	}

	@Test
	public void onInsert() throws EntityValidationException {
		T deptModel = createDepartmentTableModel();
		deptModel.items().refresh();

		Entities entities = deptModel.entities();
		deptModel.onInsert().set(EntityTableModel.OnInsert.APPEND);
		Entity dept = entities.entity(Department.TYPE)
						.with(Department.ID, -10)
						.with(Department.LOCATION, "Nowhere1")
						.with(Department.NAME, "HELLO")
						.build();
		int count = deptModel.items().included().size();
		deptModel.editor().insert(singletonList(dept));
		assertEquals(count + 1, deptModel.items().included().size());
		// Sort by name is enabled
		assertEquals(dept, deptModel.items().included().get().get(1));

		deptModel.onInsert().set(EntityTableModel.OnInsert.PREPEND);
		Entity dept2 = entities.entity(Department.TYPE)
						.with(Department.ID, -20)
						.with(Department.LOCATION, "Nowhere2")
						.with(Department.NAME, "NONAME")
						.build();
		deptModel.editor().insert(singletonList(dept2));
		assertEquals(count + 2, deptModel.items().included().size());
		assertEquals(dept2, deptModel.items().included().get().get(2));

		deptModel.onInsert().set(EntityTableModel.OnInsert.DO_NOTHING);
		Entity dept3 = entities.entity(Department.TYPE)
						.with(Department.ID, -30)
						.with(Department.LOCATION, "Nowhere3")
						.with(Department.NAME, "NONAME2")
						.build();
		deptModel.editor().insert(singletonList(dept3));
		assertEquals(count + 2, deptModel.items().included().size());

		deptModel.items().refresh();
		assertEquals(count + 3, deptModel.items().included().size());

		deptModel.editor().delete(asList(dept, dept2, dept3));
	}

	@Test
	public void removeDeletedEntities() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider);
		tableModel.items().refresh();

		Entities entities = tableModel.entities();
		Entity.Key pk1 = entities.primaryKey(Employee.TYPE, 1);
		Entity.Key pk2 = entities.primaryKey(Employee.TYPE, 2);
		tableModel.connection().startTransaction();
		try {
			tableModel.select(singletonList(pk1));
			tableModel.selection().index().set(0);
			Entity selected = tableModel.selection().item().get();
			tableModel.removeDeleted().set(true);
			tableModel.editor().tasks().delete(tableModel.selection().items().get()).perform().handle();
			assertFalse(tableModel.items().contains(selected));

			tableModel.select(singletonList(pk2));
			selected = tableModel.selection().item().get();
			tableModel.removeDeleted().set(false);
			assertEquals(1, tableModel.editor().tasks().delete(tableModel.selection().items().get()).perform().handle().size());
			assertTrue(tableModel.items().contains(selected));
		}
		finally {
			tableModel.connection().rollbackTransaction();
		}
	}

	@Test
	public void entityType() {
		assertEquals(Detail.TYPE, testModel.entityType());
	}

	@Test
	public void filterBooleanOperand() {
		// A non-nullable boolean filter operand starts at 'false', not null, so a fresh filter matches false rather than
		// an unsatisfiable null on a non-null column — mirroring the search condition model (via AttributeOperands).
		assertEquals(false, testModel.filters().get(Detail.BOOLEAN).operands().equal().get());
		// A nullable boolean stays null until set.
		assertNull(testModel.filters().get(Detail.BOOLEAN_NULLABLE).operands().equal().get());
	}

	@Test
	public void deleteNotEnabled() {
		testModel.editor().settings().deleteEnabled().set(false);
		testModel.items().refresh();
		testModel.selection().indexes().set(singletonList(0));
		assertThrows(IllegalStateException.class, () -> testModel.editor().tasks().delete(testModel.selection().items().get()).perform().handle());
	}

	@Test
	public void attributes() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider);
		tableModel.query().attributes().exclude().addAll(Employee.COMMISSION, Employee.DEPARTMENT_FK);
		tableModel.items().refresh();
		tableModel.items().get().forEach(employee -> {
			assertFalse(employee.contains(Employee.COMMISSION));
			assertFalse(employee.contains(Employee.DEPARTMENT_FK));
			assertTrue(employee.contains(Employee.NAME));
			assertTrue(employee.contains(Employee.HIREDATE));
		});
		assertThrows(IllegalArgumentException.class, () -> tableModel.query().attributes().include().add(Department.NAME));
	}

	@Test
	public void limit() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider);
		tableModel.query().limit().set(6);
		tableModel.items().refresh();
		assertEquals(6, tableModel.items().included().size());
		ConditionModel<Double> commissionCondition =
						tableModel.query().condition().get(Employee.COMMISSION);
		commissionCondition.operator().set(Operator.EQUAL);
		commissionCondition.enabled().set(true);
		tableModel.items().refresh();
		commissionCondition.enabled().set(false);
		tableModel.items().refresh();
		assertEquals(6, tableModel.items().included().size());
		tableModel.query().limit().clear();
		tableModel.items().refresh();
		assertEquals(16, tableModel.items().included().size());
	}

	@Test
	public void conditionChangedListener() {
		T empModel = createTableModel(Employee.TYPE, connectionProvider);
		AtomicInteger counter = new AtomicInteger();
		Runnable conditionChangedListener = counter::incrementAndGet;
		empModel.query().condition().modified().addListener(conditionChangedListener);
		ConditionModel<Double> commissionModel =
						empModel.query().condition().get(Employee.COMMISSION);
		commissionModel.enabled().set(true);
		assertEquals(1, counter.get());
		commissionModel.enabled().set(false);
		assertEquals(2, counter.get());
		commissionModel.set().greaterThanOrEqualTo(1200d);
		//automatically set enabled when upper bound is set
		assertEquals(3, counter.get());
		empModel.query().condition().modified().removeListener(conditionChangedListener);
	}

	@Test
	public void searchState() {
		T empModel = createTableModel(Employee.TYPE, connectionProvider);
		assertFalse(empModel.query().condition().modified().is());
		ConditionModel<String> jobModel =
						empModel.query().condition().get(Employee.JOB);
		jobModel.operands().equal().set("job");
		assertTrue(empModel.query().condition().modified().is());
		jobModel.enabled().set(false);
		assertFalse(empModel.query().condition().modified().is());
		jobModel.enabled().set(true);
		assertTrue(empModel.query().condition().modified().is());
		empModel.items().refresh();
		assertFalse(empModel.query().condition().modified().is());
	}

	protected final EntityConnectionProvider connectionProvider() {
		return connectionProvider;
	}

	@Test
	public void refreshOnForeignKeyConditionValuesSet() {
		T employeeTableModel = createTableModel(Employee.TYPE, connectionProvider());
		assertEquals(0, employeeTableModel.items().included().size());
		Entity accounting = connectionProvider().connection().selectSingle(Department.ID.equalTo(10));
		employeeTableModel.query().condition().get(Employee.DEPARTMENT_FK).set().in(accounting);
		employeeTableModel.items().refresh();
		assertEquals(7, employeeTableModel.items().included().size());
	}

	@Test
	public void filtering() {
		testModel.items().refresh();
		ConditionModel<String> filterModel = testModel.filters().get(Detail.STRING);
		filterModel.operands().equal().set("a");
		testModel.items().filter();
		assertEquals(4, testModel.items().filtered().size());
		testModel.filters().get(Detail.MASTER_FK);
	}

	@Test
	public void validItems() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		Entity dept = tableModel.entities().entity(Department.TYPE)
						.with(Department.ID, 1)
						.with(Department.NAME, "dept")
						.build();
		assertThrows(IllegalArgumentException.class, () -> tableModel.items().add(singletonList(dept)));
		assertThrows(IllegalArgumentException.class, () -> tableModel.items().included().add(0, singletonList(dept)));

		assertThrows(NullPointerException.class, () -> tableModel.items().add(singletonList(null)));
		assertThrows(NullPointerException.class, () -> tableModel.items().included().add(0, singletonList(null)));
	}

	@Test
	public void conditionChanged() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		ConditionModel<String> nameCondition = tableModel.query().condition().get(Employee.NAME);
		nameCondition.operands().equal().set(JONES);
		assertTrue(tableModel.query().condition().modified().is());
		tableModel.items().refresh();
		assertFalse(tableModel.query().condition().modified().is());
		nameCondition.enabled().set(false);
		assertTrue(tableModel.query().condition().modified().is());
		nameCondition.enabled().set(true);
		assertFalse(tableModel.query().condition().modified().is());
	}

	@Test
	public void isConditionEnabled() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		EntityQueryModel queryModel = tableModel.query();
		queryModel.conditionEnabled().set(queryModel.condition().get(Employee.MGR_FK).enabled());
		tableModel.items().refresh();
		assertEquals(16, tableModel.items().included().size());
		queryModel.conditionRequired().set(true);
		tableModel.items().refresh();
		assertEquals(0, tableModel.items().included().size());
		ConditionModel<Entity> mgrCondition = queryModel.condition().get(Employee.MGR_FK);
		mgrCondition.operands().equal().set(null);
		mgrCondition.enabled().set(true);
		tableModel.items().refresh();
		assertEquals(1, tableModel.items().included().size());
		mgrCondition.enabled().set(false);
		tableModel.items().refresh();
		assertEquals(0, tableModel.items().included().size());
	}

	@Test
	public void persistenceEvents() throws EntityValidationException {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		E employeeEditModel = tableModel.editModel();
		employeeEditModel.editor().entity().set(tableModel.items().included().get(0));
		String newName = "new name";
		employeeEditModel.editor().value(Employee.NAME).set(newName);
		E departmentEditModel = createEditModel(Department.TYPE, connectionProvider());
		departmentEditModel.editor().entity().set(employeeEditModel.editor().value(Employee.DEPARTMENT_FK).get());
		departmentEditModel.editor().value(Department.NAME).set(newName);
		EntityConnection connection = tableModel.connectionProvider().connection();
		connection.startTransaction();
		try {
			employeeEditModel.editor().update();
			assertEquals(newName, tableModel.items().included().get(0).get(Employee.NAME));
			departmentEditModel.editor().update();
			assertEquals(newName, tableModel.items().included().get(0).get(Employee.DEPARTMENT_FK).get(Department.NAME));
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	public void editorSyncedOnUpdate() throws EntityValidationException {
		// When the selected row is updated via the table (bulk/inline editing), the editor's active entity is
		// refreshed to the new values so the edit form reflects them (AbstractEntityTableModel.onUpdate).
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		E editModel = tableModel.editModel();
		tableModel.selection().index().set(0);
		Entity selected = tableModel.selection().item().get();
		assertEquals(selected.get(Employee.NAME), editModel.editor().value(Employee.NAME).get());

		EntityConnection connection = tableModel.connectionProvider().connection();
		connection.startTransaction();
		try {
			// Update the selected row via a copy, as table editing does (not the editor's own active entity).
			Entity edited = selected.copy().mutable();
			editModel.editor().value(Employee.NAME).set(edited, SYNCED);
			editModel.editor().tasks().update(singletonList(edited)).perform().handle();
			assertEquals(SYNCED, editModel.editor().value(Employee.NAME).get());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	public void editorNotSyncedOnUpdateWhenModified() throws EntityValidationException {
		// A modified editor is left untouched by a table-driven update, so unsaved edits are never overwritten.
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		E editModel = tableModel.editModel();
		tableModel.selection().index().set(0);
		Entity selected = tableModel.selection().item().get();
		editModel.editor().value(Employee.NAME).set("dirty");// unsaved edit in the editor

		EntityConnection connection = tableModel.connectionProvider().connection();
		connection.startTransaction();
		try {
			Entity edited = selected.copy().mutable();
			editModel.editor().value(Employee.NAME).set(edited, SYNCED);
			editModel.editor().tasks().update(singletonList(edited)).perform().handle();
			assertEquals("dirty", editModel.editor().value(Employee.NAME).get());
		}
		finally {
			connection.rollbackTransaction();
		}
	}

	@Test
	public void replaceByKey() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.query().attributes().exclude().set(asList(Employee.JOB, Employee.SALARY));
		tableModel.items().refresh();
		Entity.Key jonesKey = tableModel.entities().primaryKey(Employee.TYPE, 3);
		tableModel.refresh(singleton(jonesKey));
		tableModel.select(singleton(jonesKey));
		Entity selected = tableModel.selection().item().get();
		assertTrue(selected.contains(Employee.NAME));
		assertTrue(selected.contains(Employee.COMMISSION));
		assertFalse(selected.contains(Employee.JOB));
		assertFalse(selected.contains(Employee.SALARY));
	}

	@Test
	public void selectionItemsFreshAfterReplace() {
		// selection().item()/items() derive from the selected index, so replacing a selected row's entity in place must
		// surface the new values — the invariant behind the bulk-edit and editor-sync fixes.
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		tableModel.selection().index().set(0);
		Entity selected = tableModel.selection().item().get();
		String originalName = selected.get(Employee.NAME);
		Entity replacement = selected.copy().mutable();
		replacement.set(Employee.NAME, REPLACED);
		tableModel.replace(singletonList(replacement));
		assertEquals(REPLACED, tableModel.selection().item().get().get(Employee.NAME));
		assertEquals(REPLACED, tableModel.selection().items().get().get(0).get(Employee.NAME));
		assertNotEquals(originalName, tableModel.selection().item().get().get(Employee.NAME));
	}

	@Test
	public void selectionAfterFilter() {
		// Filtering a selected row out of the included set drops it from the selection; still-included rows stay selected.
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		tableModel.selection().selectAll();
		Entity jones = tableModel.selection().items().get().stream()
						.filter(employee -> JONES.equals(employee.get(Employee.NAME)))
						.findFirst()
						.orElseThrow(IllegalStateException::new);
		tableModel.filters().get(Employee.NAME).operands().equal().set(JONES);
		tableModel.items().filter();
		assertTrue(tableModel.selection().items().get().contains(jones));
		tableModel.selection().items().get().forEach(selected ->
						assertTrue(tableModel.items().included().contains(selected)));
	}

	@Test
	public void selectionAfterSort() {
		// Sorting keeps the same entity selected (selection follows its row); its index reflects the new position.
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		tableModel.selection().index().set(0);
		Entity selected = tableModel.selection().item().get();
		tableModel.sort().descending(Employee.NAME);
		assertEquals(selected, tableModel.selection().item().get());
		tableModel.sort().ascending(Employee.NAME);
		assertEquals(selected, tableModel.selection().item().get());
		assertEquals(tableModel.items().included().indexOf(selected), tableModel.selection().index().get().intValue());
	}

	@Test
	public void singleAndMultipleSelection() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		MultiSelection<Entity> selection = tableModel.selection();
		assertTrue(selection.empty().is());
		assertEquals(0, selection.count());
		assertFalse(selection.single().is());
		assertFalse(selection.multiple().is());

		selection.index().set(0);
		assertFalse(selection.empty().is());
		assertTrue(selection.single().is());
		assertFalse(selection.multiple().is());
		assertEquals(1, selection.count());

		selection.indexes().set(asList(0, 1, 2));
		assertTrue(selection.multiple().is());
		assertFalse(selection.single().is());
		assertEquals(3, selection.count());

		selection.clear();
		assertTrue(selection.empty().is());
		assertEquals(0, selection.count());
	}

	@Test
	public void selectionNavigation() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		MultiSelection<Entity> selection = tableModel.selection();

		selection.selectAll();
		assertEquals(tableModel.items().included().size(), selection.count());
		selection.clear();
		assertTrue(selection.empty().is());

		selection.index().set(0);
		selection.indexes().increment();
		assertEquals(1, selection.index().get().intValue());
		selection.indexes().decrement();
		assertEquals(0, selection.index().get().intValue());

		selection.indexes().add(2);
		assertTrue(selection.indexes().contains(2));
		assertEquals(asList(0, 2), selection.indexes().get());
		selection.indexes().remove(0);
		assertFalse(selection.indexes().contains(0));
		assertEquals(singletonList(2), selection.indexes().get());
	}

	@Test
	public void sortAscendingDescending() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		tableModel.sort().ascending(Employee.NAME);
		assertSortedByName(tableModel.items().included().get(), true);
		tableModel.sort().descending(Employee.NAME);
		assertSortedByName(tableModel.items().included().get(), false);
	}

	@Test
	public void sortClear() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		tableModel.sort().ascending(Employee.NAME);
		assertEquals(SortOrder.ASCENDING, tableModel.sort().columns().get(Employee.NAME).sortOrder());
		tableModel.sort().clear();
		assertEquals(SortOrder.UNSORTED, tableModel.sort().columns().get(Employee.NAME).sortOrder());
		assertTrue(tableModel.sort().columns().get().isEmpty());
	}

	@Test
	public void sortPriority() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		tableModel.sort().order(Employee.JOB).set(SortOrder.ASCENDING);
		tableModel.sort().order(Employee.NAME).add(SortOrder.DESCENDING);
		// JOB is the primary sort column (priority 0), NAME the secondary (priority 1).
		assertEquals(0, tableModel.sort().columns().get(Employee.JOB).priority());
		assertEquals(1, tableModel.sort().columns().get(Employee.NAME).priority());
		assertEquals(2, tableModel.sort().columns().get().size());
		assertEquals(Employee.JOB, tableModel.sort().columns().get().get(0).identifier());
		// Rows are ordered by JOB ascending, then NAME descending within an equal JOB.
		List<Entity> items = tableModel.items().included().get();
		for (int i = 1; i < items.size(); i++) {
			Entity previous = items.get(i - 1);
			Entity current = items.get(i);
			int jobComparison = previous.get(Employee.JOB).compareTo(current.get(Employee.JOB));
			assertTrue(jobComparison <= 0);
			if (jobComparison == 0) {
				assertTrue(previous.get(Employee.NAME).compareTo(current.get(Employee.NAME)) >= 0);
			}
		}
	}

	@Test
	public void filterNotEqual() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		int total = tableModel.items().included().size();
		ConditionModel<String> jobFilter = tableModel.filters().get(Employee.JOB);
		jobFilter.operator().set(Operator.NOT_EQUAL);
		jobFilter.operands().equal().set("SALESMAN");
		tableModel.items().filter();
		assertTrue(tableModel.items().included().size() < total);
		tableModel.items().included().get().forEach(employee ->
						assertNotEquals("SALESMAN", employee.get(Employee.JOB)));
	}

	@Test
	public void filterEnabled() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		int total = tableModel.items().included().size();
		ConditionModel<String> jobFilter = tableModel.filters().get(Employee.JOB);
		jobFilter.operands().equal().set("CLERK");
		tableModel.items().filter();
		assertTrue(tableModel.items().included().size() < total);
		jobFilter.enabled().set(false);
		tableModel.items().filter();
		assertEquals(total, tableModel.items().included().size());
	}

	private static void assertSortedByName(List<Entity> items, boolean ascending) {
		for (int i = 1; i < items.size(); i++) {
			int comparison = items.get(i - 1).get(Employee.NAME).compareTo(items.get(i).get(Employee.NAME));
			assertTrue(ascending ? comparison <= 0 : comparison >= 0);
		}
	}

	@Test
	public void replace() {
		T tableModel = createTableModel(Employee.TYPE, connectionProvider());
		tableModel.items().refresh();
		Entity employee = tableModel.items().included().get().get(0);
		Entity replacement = employee.copy().mutable();
		replacement.set(Employee.NAME, "REPLACED");
		tableModel.replace(singletonList(replacement));
		Entity updated = tableModel.items().included().get().stream()
						.filter(entity -> entity.primaryKey().equals(employee.primaryKey()))
						.findFirst()
						.orElseThrow(IllegalStateException::new);
		assertEquals("REPLACED", updated.get(Employee.NAME));
	}

	/**
	 * @return a EntityTableModel using {@link #testEntities} with an edit model
	 * @see Detail#TYPE
	 */
	protected abstract T createTestTableModel();

	protected abstract T createDepartmentTableModel();

	protected abstract T createTableModel(EntityType entityType, EntityConnectionProvider connectionProvider);

	protected abstract T createTableModel(E editModel);

	protected abstract E createEditModel(EntityType entityType, EntityConnectionProvider connectionProvider);

	private static List<Entity> initTestEntities(Entities entities) {
		List<Entity> testEntities = new ArrayList<>(5);
		String[] stringValues = new String[] {"a", "b", "c", "d", "e"};
		for (int i = 0; i < 5; i++) {
			testEntities.add(entities.entity(Detail.TYPE)
							.with(Detail.ID, (long) i + 1)
							.with(Detail.INT, i + 1)
							.with(Detail.STRING, stringValues[i])
							.build());
		}

		return testEntities;
	}
}