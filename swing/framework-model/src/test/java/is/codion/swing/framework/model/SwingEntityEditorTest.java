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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.model;

import is.codion.common.model.CancelException;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.EditorLink;
import is.codion.framework.model.EntityEditor;
import is.codion.framework.model.test.TestDomain;
import is.codion.framework.model.test.TestDomain.Department;
import is.codion.framework.model.test.TestDomain.Employee;

import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.swing.SwingUtilities;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.synchronizedList;
import static java.util.concurrent.TimeUnit.SECONDS;
import static javax.swing.SwingUtilities.invokeLater;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Validates {@link SwingEntityEditor} event ordering and asynchronous behaviour.
 * <p>The ordering/semantics tests (Layer A) run on the calling thread, where
 * {@link SwingEntityEditor#execute(EntityEditor.EditorTask)} is synchronous, and simply assert the
 * recorded event sequence. The asynchronous tests (Layer B) trigger operations on the Event Dispatch
 * Thread via {@code invokeLater} and await a {@link CountDownLatch} counted down by the terminal
 * event, the only way to observe the worker-based path.
 * <p>Detail loading is stubbed with an in-memory entity ({@link #detailToLoad}) so the assertions
 * are deterministic and independent of database content.
 */
public final class SwingEntityEditorTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new TestDomain())
					.user(UNIT_TEST_USER)
					.build();

	private final Entities entities = CONNECTION_PROVIDER.entities();
	private final List<String> events = synchronizedList(new ArrayList<>());
	private final AtomicReference<@Nullable Entity> detailToLoad = new AtomicReference<>();

	private SwingEntityEditor master;
	private SwingEntityEditor detail;

	private SwingEntityModel departmentModel;
	private SwingEntityModel employeeModel;

	@BeforeEach
	void setUp() {
		events.clear();
		detailToLoad.set(null);
		master = new SwingEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		detail = new SwingEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		master.detail().add(EditorLink.builder()
						.editor(detail)
						.foreignKey(Employee.DEPARTMENT_FK)
						// Stubbed load: returns the controllable in-memory detail, never queries
						.entity((department, connection) -> detailToLoad.get())
						.present(employee -> !employee.isNull(Employee.NAME))
						.build());
		departmentModel = new SwingEntityModel(Department.TYPE, CONNECTION_PROVIDER);
		employeeModel = new SwingEntityModel(Employee.TYPE, CONNECTION_PROVIDER);
		departmentModel.detail().add(employeeModel);
		departmentModel.detail().active(employeeModel).set(true);
		departmentModel.tableModel().items().refresh();
		record(master, "master");
		record(detail, "detail");
	}

	// Layer A — synchronous (off the EDT), validating event ordering and which event fires

	@Test
	void setOrdering() {
		detailToLoad.set(employee("emp"));
		master.entity().set(department(1));
		// changing fires first; the detail is set (changed, never changing); the master changes last
		assertEquals(asList("master.changing", "detail.changed", "master.changed"), events);
	}

	@Test
	void defaultsResetsDetailSilently() {
		detailToLoad.set(employee("emp"));
		master.entity().set(department(1));
		events.clear();
		master.entity().defaults();
		// master fires changing + changed; the detail is reset silently, firing only replaced
		assertEquals(asList("master.changing", "detail.replaced", "master.changed"), events);
	}

	@Test
	void clearResetsDetailSilently() {
		detailToLoad.set(employee("emp"));
		master.entity().set(department(1));
		events.clear();
		master.entity().clear();
		assertEquals(asList("master.changing", "detail.replaced", "master.changed"), events);
	}

	@Test
	void replaceSamePrimaryKeyIsSilentAndSkipsDetail() {
		detailToLoad.set(employee("emp"));
		master.entity().set(department(1));
		events.clear();
		// Same primary key: detail is not touched, only the master's replaced fires
		master.entity().replace(department(1));
		assertEquals(singletonList("master.replaced"), events);
	}

	@Test
	void replaceIdentityChangedReloadsDetail() {
		detailToLoad.set(employee("emp"));
		master.entity().set(department(1));
		events.clear();
		detailToLoad.set(employee("emp2"));
		// Primary key changes: the detail is reloaded (replaced), then the master replaced fires
		master.entity().replace(department(2));
		assertEquals(asList("detail.replaced", "master.replaced"), events);
	}

	@Test
	void vetoCancelsSet() {
		master.entity().changing().addConsumer(department -> {
			throw new CancelException();
		});
		detailToLoad.set(employee("emp"));
		assertThrows(CancelException.class, () -> master.entity().set(department(1)));
		// The change was vetoed before loading: no changed events, the entity is untouched
		assertFalse(events.contains("master.changed"));
		assertFalse(events.contains("detail.changed"));
		assertTrue(master.entity().get().primaryKey().isNull());
	}

	// Layer B — asynchronous (on the EDT), validating the worker-based path

	@Test
	void asyncAppliesValuesSynchronouslyDefersChanged() throws Exception {
		detailToLoad.set(employee("emp"));
		AtomicReference<@Nullable Integer> idRightAfterSet = new AtomicReference<>();
		AtomicBoolean changedRightAfterSet = new AtomicBoolean(true);
		AtomicBoolean changedFired = new AtomicBoolean();
		CountDownLatch changed = new CountDownLatch(1);
		master.entity().observer().addListener(() -> {
			changedFired.set(true);
			changed.countDown();
		});
		invokeLater(() -> {
			master.entity().set(department(1));
			// The master's own values are applied synchronously, before the detail load is dispatched
			idRightAfterSet.set(master.entity().get().get(Department.ID));
			// but the changed event is deferred until the detail editors have loaded on the worker
			changedRightAfterSet.set(changedFired.get());
		});
		assertTrue(changed.await(5, SECONDS));
		assertEquals(1, idRightAfterSet.get());// applied synchronously
		assertFalse(changedRightAfterSet.get());// changed not yet fired on return, detail still loading
		assertEquals(1, master.entity().get().get(Department.ID));
	}

	@Test
	void supersededSetIsDiscarded() throws Exception {
		List<@Nullable Integer> changedIds = synchronizedList(new ArrayList<>());
		CountDownLatch secondApplied = new CountDownLatch(1);
		master.entity().observer().addConsumer(department -> {
			Integer id = department.get(Department.ID);
			changedIds.add(id);
			if (Objects.equals(id, 2)) {
				secondApplied.countDown();
			}
		});
		detailToLoad.set(employee("emp"));
		// Two sets queued back to back: the second supersedes the first before any worker completes
		invokeLater(() -> master.entity().set(department(1)));
		invokeLater(() -> master.entity().set(department(2)));
		assertTrue(secondApplied.await(5, SECONDS));
		// The superseded set(1) never applied; only set(2) won (regardless of completion order)
		assertEquals(singletonList(2), changedIds);
		assertEquals(2, master.entity().get().get(Department.ID));
	}

	@Test
	void asyncDisabledIsSynchronous() throws Exception {
		master.async().set(false);
		detailToLoad.set(employee("emp"));
		AtomicReference<@Nullable Integer> idRightAfterSet = new AtomicReference<>();
		CountDownLatch done = new CountDownLatch(1);
		invokeLater(() -> {
			master.entity().set(department(1));
			// Async disabled: set() completed synchronously, the entity is already applied on return
			idRightAfterSet.set(master.entity().get().get(Department.ID));
			done.countDown();
		});
		assertTrue(done.await(5, SECONDS));
		assertEquals(1, idRightAfterSet.get());
	}

	// Layer A — persistence ordering (synchronous, in a rolled-back transaction)

	@Test
	void insertWithDetailOrdering() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			master.value(Department.ID).set(99);
			master.value(Department.NAME).set("TST");
			detail.value(Employee.NAME).set("emp");
			detail.value(Employee.SALARY).set(1000d);
			detail.value(Employee.HIREDATE).set(LocalDate.of(2020, 1, 1));
			events.clear();
			master.insert();
			// The detail is persisted and reaches its post-persist state (replaced) before the master's
			assertEquals(asList("detail.replaced", "master.replaced"), events);
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void updateWithDetailOrdering() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			master.value(Department.ID).set(99);
			master.value(Department.NAME).set("TST");
			detail.value(Employee.NAME).set("emp");
			detail.value(Employee.SALARY).set(1000d);
			detail.value(Employee.HIREDATE).set(LocalDate.of(2020, 1, 1));
			master.insert();
			// Modify both master and detail so the detail participates in the update
			master.value(Department.NAME).set("UPD");
			detail.value(Employee.SALARY).set(2000d);
			events.clear();
			master.update();
			assertEquals(asList("detail.replaced", "master.replaced"), events);
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void deleteResetsToDefaultsSilently() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			master.value(Department.ID).set(99);
			master.value(Department.NAME).set("TST");
			master.insert();// detail editor empty (not present), only the department is inserted
			assertTrue(master.entity().exists().is());
			events.clear();
			master.delete();
			// The post-delete reset fires replaced, never changing — so no spurious modified-warning veto
			assertEquals(asList("detail.replaced", "master.replaced"), events);
			assertFalse(events.contains("master.changing"));
			assertFalse(master.entity().exists().is());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void sync() {
		EntityConnection connection = CONNECTION_PROVIDER.connection();
		Entity research = connection.selectSingle(Department.NAME.equalTo("RESEARCH"));
		Entity sales = connection.selectSingle(Department.NAME.equalTo("SALES"));
		Entity martin = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
		// Off the EDT both the detail refresh and the editor run synchronously, so the
		// foreign key is settled by the time each assertion runs.

		departmentModel.tableModel().selection().item().set(research);
		assertEquals(research, foreignKey());

		departmentModel.tableModel().selection().item().set(sales);
		assertEquals(sales, foreignKey());

		employeeModel.tableModel().selection().item().set(martin);

		departmentModel.tableModel().selection().item().set(research);
		assertEquals(research, foreignKey());
	}

	@Test
	void async() throws Exception {
		EntityConnection connection = CONNECTION_PROVIDER.connection();
		Entity research = connection.selectSingle(Department.NAME.equalTo("RESEARCH"));
		Entity sales = connection.selectSingle(Department.NAME.equalTo("SALES"));
		Entity martin = connection.selectSingle(Employee.NAME.equalTo("MARTIN"));
		// On the EDT the detail refresh runs asynchronously. With Direction A the editor applies its
		// own values (and defaults/clear) synchronously, so once the async refresh has settled the
		// foreign key set by the link in the refresh onResult is the final value.

		// Selecting a department sets the detail condition and triggers the async detail refresh,
		// whose onResult sets the foreign key. Await the refresh, then assert the settled value.
		awaitRefresh(() -> departmentModel.tableModel().selection().item().set(research));
		assertForeignKey(research);

		awaitRefresh(() -> departmentModel.tableModel().selection().item().set(sales));
		assertForeignKey(sales);

		// Selecting an employee populates the employee editor (values applied synchronously).
		onEdt(() -> employeeModel.tableModel().selection().item().set(martin));

		// Re-selecting a department refreshes the employee table, clearing the now-invalid employee
		// selection (Martin is in Sales), which resets the editor to defaults. Because defaults() is
		// synchronous, it runs before the refresh onResult, so the foreign key set by the link wins.
		awaitRefresh(() -> departmentModel.tableModel().selection().item().set(research));
		assertForeignKey(research);
	}

	private void awaitRefresh(Runnable gesture) throws Exception {
		CountDownLatch refreshed = new CountDownLatch(1);
		Runnable listener = refreshed::countDown;
		// result() fires after the refresh has processed and the link's onResult has set the foreign key
		employeeModel.tableModel().items().refresher().result().addListener(listener);
		try {
			SwingUtilities.invokeLater(gesture);
			assertTrue(refreshed.await(10, SECONDS), "Detail refresh did not complete");
			flushEventQueue();
		}
		finally {
			employeeModel.tableModel().items().refresher().result().removeListener(listener);
		}
	}

	private void assertForeignKey(Entity department) throws Exception {
		onEdt(() -> assertEquals(department, foreignKey()));
	}

	private Entity foreignKey() {
		return employeeModel.editModel().editor().value(Employee.DEPARTMENT_FK).get();
	}

	private static void onEdt(Runnable runnable) throws Exception {
		SwingUtilities.invokeAndWait(runnable);
	}

	private static void flushEventQueue() throws Exception {
		SwingUtilities.invokeAndWait(() -> {});
	}

	private void record(SwingEntityEditor editor, String label) {
		editor.entity().changing().addListener(() -> events.add(label + ".changing"));
		editor.entity().observer().addListener(() -> events.add(label + ".changed"));
		editor.entity().replaced().addListener(() -> events.add(label + ".replaced"));
	}

	private Entity department(int id) {
		return entities.entity(Department.TYPE)
						.with(Department.ID, id)
						.with(Department.NAME, "dept" + id)
						.build();
	}

	private Entity employee(String name) {
		return entities.entity(Employee.TYPE)
						.with(Employee.NAME, name)
						.with(Employee.SALARY, 1000d)
						.build();
	}
}
