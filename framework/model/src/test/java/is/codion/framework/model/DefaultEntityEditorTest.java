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
package is.codion.framework.model;

import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.local.LocalEntityConnectionProvider;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.DefaultEntityEditor.DefaultDetailEditors;
import is.codion.framework.model.DefaultEntityEditor.DetailForeignKeyValidator;
import is.codion.framework.model.DetailDomain.Department;
import is.codion.framework.model.DetailDomain.DepartmentExtra;
import is.codion.framework.model.DetailDomain.Employee;
import is.codion.framework.model.EntityEditor.EditorValue;

import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;

import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.condition.Condition.and;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultEntityEditorTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnectionProvider CONNECTION_PROVIDER = LocalEntityConnectionProvider.builder()
					.domain(new DetailDomain())
					.user(UNIT_TEST_USER)
					.build();

	private static final Predicate<Entity> EMPLOYEE_PRESENT = employee ->
					!employee.isNull(Employee.SALARY) && !employee.isNull(Employee.NAME);

	@Test
	void simpleDetailEditor() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			EditorValue<Integer> departmentId = departmentEditor.value(Department.ID);
			EditorValue<String> departmentName = departmentEditor.value(Department.NAME);

			EditorValue<String> employeeName = employeeEditor.value(Employee.NAME);
			EditorValue<Double> salary = employeeEditor.value(Employee.SALARY);

			departmentId.set(42);
			departmentName.set("Test");
			employeeName.set("Test");
			salary.set(2000d);

			insert(departmentEditor);
			assertTrue(departmentEditor.exists().is());
			assertTrue(employeeEditor.exists().is());

			salary.clear();//now invalid
			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);
			assertFalse(employeeEditor.exists().is());

			employeeName.set("Test");
			salary.set(2100d);// valid again

			update(departmentEditor);
			assertTrue(employeeEditor.exists().is());

			salary.set(1990d);// present but invalid
			assertThrows(EntityValidationException.class, () -> insert(departmentEditor));

			delete(departmentEditor);
			assertFalse(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void threeLevelDetailEditor() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());
			TestEntityEditor managerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			employeeEditor.detail().add(EditorLink.builder()
							.editor(managerEditor)
							.foreignKey(Employee.MANAGER_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			EditorValue<Integer> departmentId = departmentEditor.value(Department.ID);
			EditorValue<String> departmentName = departmentEditor.value(Department.NAME);

			EditorValue<String> employeeName = employeeEditor.value(Employee.NAME);
			EditorValue<Double> employeeSalary = employeeEditor.value(Employee.SALARY);

			EditorValue<String> managerName = managerEditor.value(Employee.NAME);
			EditorValue<Double> managerSalary = managerEditor.value(Employee.SALARY);
			EditorValue<Integer> managerDepartment = managerEditor.value(Employee.DEPARTMENT);

			departmentId.set(42);
			departmentName.set("Test");
			employeeName.set("Test");
			employeeSalary.set(2000d);
			managerName.set("TestMgr");
			managerSalary.set(3000d);
			managerDepartment.set(10);

			insert(departmentEditor);
			assertTrue(departmentEditor.exists().is());
			assertTrue(employeeEditor.exists().is());
			assertTrue(managerEditor.exists().is());

			employeeSalary.clear();//now invalid
			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);
			assertFalse(employeeEditor.exists().is());
			assertFalse(managerEditor.exists().is());

			employeeName.set("Test");
			employeeSalary.set(2100d);// valid again

			update(departmentEditor);
			assertTrue(employeeEditor.exists().is());
			assertFalse(managerEditor.exists().is());

			managerName.set("TestMgr2");
			managerSalary.set(3100d);// valid again
			managerDepartment.set(10);

			assertTrue(employeeEditor.modified().is());
			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);
			assertTrue(employeeEditor.exists().is());
			assertTrue(managerEditor.exists().is());

			delete(departmentEditor);
			assertFalse(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());
			assertFalse(managerEditor.exists().is());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorSetup() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build());

		// FK persist is false
		assertFalse(employeeEditor.value(Employee.DEPARTMENT_FK).persist().is());
		// Validator is wrapped in DetailForeignKeyValidator
		assertInstanceOf(DetailForeignKeyValidator.class, employeeEditor.validator().get());
		// Detail is initially clear
		assertFalse(employeeEditor.exists().is());
		assertFalse(employeeEditor.modified().is());
		assertNull(employeeEditor.value(Employee.NAME).get());
		assertNull(employeeEditor.value(Employee.SALARY).get());
		// Master's modified().additional() contains the detail's updatable state
		assertFalse(departmentEditor.modified().additional().get().isEmpty());
		assertThrows(IllegalArgumentException.class, () -> employeeEditor.validator().set(new EntityValidator() {}));
	}

	@Test
	void detailEditorModifiedAttributesStayFresh() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			departmentEditor.value(Department.ID).set(42);
			departmentEditor.value(Department.NAME).set("Test");
			employeeEditor.value(Employee.NAME).set("Original");
			employeeEditor.value(Employee.SALARY).set(2000d);
			insert(departmentEditor);

			// Modify two detail attributes — both should appear in master's modified attributes.
			employeeEditor.value(Employee.NAME).set("Modified");
			employeeEditor.value(Employee.SALARY).set(2100d);
			assertTrue(departmentEditor.modified().attributes().get().contains(Employee.NAME));
			assertTrue(departmentEditor.modified().attributes().get().contains(Employee.SALARY));

			// Revert NAME back to original, keep SALARY modified. The detail's Updatable.update
			// state stays true (no flip — still present, exists, and modified via SALARY), but
			// the modified-attributes set must refresh to drop NAME.
			employeeEditor.value(Employee.NAME).set("Original");
			assertFalse(departmentEditor.modified().attributes().get().contains(Employee.NAME));
			assertTrue(departmentEditor.modified().attributes().get().contains(Employee.SALARY));

			delete(departmentEditor);
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorValidReflectsContext() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build());

		// Required non-FK fields (NAME, SALARY) are unset
		assertFalse(employeeEditor.valid().is());

		// Populate the required non-FK fields. The FK is still null,
		// but it's framework-managed so the editor is now valid.
		employeeEditor.value(Employee.NAME).set("Test");
		employeeEditor.value(Employee.SALARY).set(2000d);
		assertTrue(employeeEditor.valid().is());

		// Clearing a required non-FK field still makes the editor invalid —
		// the wrapper only suppresses null-FK, not other null-required-attribute errors.
		employeeEditor.value(Employee.NAME).clear();
		assertFalse(employeeEditor.valid().is());
	}

	@Test
	void nonForeignKeyDetailEditor() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);

			AtomicInteger beforeInsertCalls = new AtomicInteger();
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.name("first-employee")
							.condition(Employee.DEPARTMENT_FK::equalTo)
							.beforeInsert((emp, dept, connection) -> {
								beforeInsertCalls.incrementAndGet();
								emp.set(Employee.DEPARTMENT, dept.get(Department.ID));
							})
							.present(EMPLOYEE_PRESENT)
							.build());

			// Non-FK link: validator is NOT wrapped, FK persist is NOT touched
			assertFalse(employeeEditor.validator().get() instanceof DetailForeignKeyValidator);
			assertTrue(employeeEditor.value(Employee.DEPARTMENT_FK).persist().is());
			// Lookup by name works
			assertSame(employeeEditor, departmentEditor.detail().get("first-employee"));

			// Insert: detail editor must be fully populated (no FK-management to suppress null-FK validation)
			departmentEditor.value(Department.ID).set(61);
			departmentEditor.value(Department.NAME).set("NonFK");
			employeeEditor.value(Employee.NAME).set("Solo");
			employeeEditor.value(Employee.SALARY).set(2500d);
			employeeEditor.value(Employee.DEPARTMENT).set(61);

			insert(departmentEditor);
			assertTrue(departmentEditor.exists().is());
			assertTrue(employeeEditor.exists().is());
			assertEquals(1, beforeInsertCalls.get());
			assertEquals(61, employeeEditor.entity().get().get(Employee.DEPARTMENT));

			// Master changes -> detail loads via user-supplied condition
			EntityConnection connection = CONNECTION_PROVIDER.connection();
			Entity dept62 = connection.insertSelect(
							CONNECTION_PROVIDER.entities().entity(Department.TYPE)
											.with(Department.ID, 62)
											.with(Department.NAME, "Empty")
											.build());
			departmentEditor.entity().set(dept62);
			assertFalse(employeeEditor.exists().is());

			Entity dept61 = connection.selectSingle(Department.ID.equalTo(61));
			departmentEditor.entity().set(dept61);
			assertTrue(employeeEditor.exists().is());
			assertEquals("Solo", employeeEditor.value(Employee.NAME).get());

			// Update path
			employeeEditor.value(Employee.SALARY).set(2700d);
			update(departmentEditor);
			assertEquals(2700d, connection.selectSingle(Employee.NAME.equalTo("Solo")).get(Employee.SALARY));

			// Delete cascades to detail
			delete(departmentEditor);
			assertFalse(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void foreignKeyDetailEditorNameOverride() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor managerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor headEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);

		// Two FK-based detail editors sharing the same FK, distinguished by an explicit name override
		DefaultDetailEditors detail = (DefaultDetailEditors) departmentEditor.detail();
		detail.add(EditorLink.builder()
						.editor(managerEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.name("manager")
						.build());
		detail.add(EditorLink.builder()
						.editor(headEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.name("head")
						.build());

		// Lookup goes through the explicit names; FK-based default lookup would have collided
		assertSame(managerEditor, detail.get("manager"));
		assertSame(headEditor, detail.get("head"));

		// FK-based lookup throws when multiple links share the same FK — must use get(name)
		IllegalStateException ambiguous = assertThrows(IllegalStateException.class,
						() -> detail.get(Employee.DEPARTMENT_FK));
		assertTrue(ambiguous.getMessage().contains("manager"));
		assertTrue(ambiguous.getMessage().contains("head"));
		assertThrows(IllegalStateException.class,
						() -> detail.name(Employee.DEPARTMENT_FK));

		// Empty name override is rejected
		TestEntityEditor employee = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		assertThrows(IllegalArgumentException.class, () -> EditorLink.builder()
						.editor(employee)
						.foreignKey(Employee.DEPARTMENT_FK)
						.name("")
						.build());
	}

	@Test
	void foreignKeyDetailEditorLookupRespectsNameOverride() {
		// Single FK detail editor with an explicit name override — get(ForeignKey) must still
		// find it (was previously a sugar over get(foreignKey.name()) which silently returned null
		// for renamed links).
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor managerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		DefaultDetailEditors detail = (DefaultDetailEditors) departmentEditor.detail();
		detail.add(EditorLink.builder()
						.editor(managerEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.name("Manager")
						.build());

		assertSame(managerEditor, detail.get(Employee.DEPARTMENT_FK));
		assertSame(managerEditor, detail.get("Manager"));
		assertEquals("Manager", detail.name(Employee.DEPARTMENT_FK));

		// Unregistered FK throws
		assertThrows(IllegalStateException.class,
						() -> detail.get(DepartmentExtra.DEPARTMENT_FK));
		assertThrows(IllegalStateException.class,
						() -> detail.name(DepartmentExtra.DEPARTMENT_FK));
	}

	@Test
	void detailEditorsWithSameNameButDifferentForeignKeys() {
		// Two FK-based detail editors with the same explicit name but different foreign keys
		// should coexist — the (FK, name) tuple distinguishes them. Motivated by a copy-paste
		// scenario where three FK links were given the same .name() by accident.
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION_PROVIDER);

		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.name("X")
						.build());
		departmentEditor.detail().add(EditorLink.builder()
						.editor(extraEditor)
						.foreignKey(DepartmentExtra.DEPARTMENT_FK)
						.name("X")
						.build());

		// Lookup by FK uniquely identifies each
		assertSame(employeeEditor, departmentEditor.detail().get(Employee.DEPARTMENT_FK));
		assertSame(extraEditor, departmentEditor.detail().get(DepartmentExtra.DEPARTMENT_FK));

		// Lookup by name is now ambiguous — must use get(FK)
		IllegalStateException ambiguous = assertThrows(IllegalStateException.class,
						() -> departmentEditor.detail().get("X"));
		assertTrue(ambiguous.getMessage().contains("X"));
		assertTrue(ambiguous.getMessage().contains("get(ForeignKey)"));

		// True duplicate (same FK + same name) still rejected
		TestEntityEditor duplicateEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		assertThrows(IllegalArgumentException.class, () ->
						departmentEditor.detail().add(EditorLink.builder()
										.editor(duplicateEditor)
										.foreignKey(Employee.DEPARTMENT_FK)
										.name("X")
										.build()));
	}

	@Test
	void editorLinkCaption() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor managerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor headEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor plainEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);

		// Explicit caption used for UI display while name remains the structural identifier
		DefaultDetailEditors detail = (DefaultDetailEditors) departmentEditor.detail();
		detail.add(EditorLink.builder()
						.editor(managerEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.name("manager")
						.caption("Manager")
						.build());
		// No caption override — caption() falls back to name()
		detail.add(EditorLink.builder()
						.editor(headEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.name("head")
						.build());
		// Non-FK link with caption override
		detail.add(EditorLink.builder()
						.editor(plainEditor)
						.name("plain")
						.condition(Employee.DEPARTMENT_FK::equalTo)
						.caption("Plain Caption")
						.build());

		assertEquals("Manager", detail.caption("manager"));
		assertEquals("head", detail.caption("head"));
		assertEquals("Plain Caption", detail.caption("plain"));

		// Unknown name throws
		assertThrows(IllegalStateException.class,
						() -> detail.caption("unknown"));

		// Empty/null caption rejected
		TestEntityEditor employee = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		assertThrows(IllegalArgumentException.class, () -> EditorLink.builder()
						.editor(employee)
						.foreignKey(Employee.DEPARTMENT_FK)
						.caption(""));
		assertThrows(IllegalArgumentException.class, () -> EditorLink.builder()
						.editor(employee)
						.foreignKey(Employee.DEPARTMENT_FK)
						.caption(null));
	}

	@Test
	void detailValidatorReplacementPrevented() {
		// FK-based link: validator is wrapped and locked
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.build());

		assertInstanceOf(DetailForeignKeyValidator.class, employeeEditor.validator().get());

		EntityValidator replacement = new EntityValidator() {};
		assertThrows(IllegalArgumentException.class, () -> employeeEditor.validator().set(replacement));
		assertThrows(IllegalArgumentException.class, () ->
						employeeEditor.validator().update(current -> replacement));

		// Non-FK link: no FK management, validator can be replaced freely
		TestEntityEditor otherDept = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor otherEmp = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		otherDept.detail().add(EditorLink.builder()
						.editor(otherEmp)
						.name("non-fk")
						.condition(Employee.DEPARTMENT_FK::equalTo)
						.build());

		otherEmp.validator().set(replacement);
		assertSame(replacement, otherEmp.validator().get());
	}

	@Test
	void detailEditorSetupValidation() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION_PROVIDER);

		// Wrong detail entity type: DepartmentExtra editor with Employee foreign key
		assertThrows(IllegalArgumentException.class, () ->
						departmentEditor.detail().add(EditorLink.builder()
										.editor(extraEditor)
										.foreignKey(Employee.DEPARTMENT_FK)
										.build()));
		// Wrong master entity type: Employee FK references Department, but using employee as master
		TestEntityEditor anotherEmployeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		assertThrows(IllegalArgumentException.class, () ->
						anotherEmployeeEditor.detail().add(EditorLink.builder()
										.editor(employeeEditor)
										.foreignKey(Employee.DEPARTMENT_FK)
										.build()));
	}

	@Test
	void detailEditorMasterChanged() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master with detail
			departmentEditor.value(Department.ID).set(42);
			departmentEditor.value(Department.NAME).set("Test");
			employeeEditor.value(Employee.NAME).set("Emp1");
			employeeEditor.value(Employee.SALARY).set(2000d);

			insert(departmentEditor);
			assertTrue(employeeEditor.exists().is());

			// Insert another department
			EntityConnection connection = CONNECTION_PROVIDER.connection();
			Entity dept43 = connection.insertSelect(
							CONNECTION_PROVIDER.entities().entity(Department.TYPE)
											.with(Department.ID, 43)
											.with(Department.NAME, "Other")
											.build());

			// Set a different entity on master -> detail should load from DB (no match for dept 43)
			departmentEditor.entity().set(dept43);
			assertFalse(employeeEditor.exists().is());

			// Set master back to original dept -> detail loads from DB
			Entity dept42 = connection.selectSingle(Department.ID.equalTo(42));
			departmentEditor.entity().set(dept42);
			assertTrue(employeeEditor.exists().is());
			assertEquals("Emp1", employeeEditor.value(Employee.NAME).get());

			// Set master entity to null -> detail is cleared
			departmentEditor.entity().set(null);
			assertFalse(employeeEditor.exists().is());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdatableStates() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build());

		// masterExists=false -> master modified is false regardless
		assertFalse(departmentEditor.exists().is());
		employeeEditor.value(Employee.NAME).set("Test");
		employeeEditor.value(Employee.SALARY).set(1000d);
		assertFalse(departmentEditor.modified().is());

		// Simulate master existing by setting an existing entity (dept 40 has no employees)
		Entity existingDept = CONNECTION_PROVIDER.connection()
						.selectSingle(Department.ID.equalTo(40));
		departmentEditor.entity().set(existingDept);
		assertTrue(departmentEditor.exists().is());

		// masterExists=true, valid=true, exists=false -> insert state (detail is new and valid)
		// The employee editor was cleared by masterChanged (dept 40 has no detail employee)
		employeeEditor.value(Employee.NAME).set("Test");
		employeeEditor.value(Employee.SALARY).set(2000d);
		assertTrue(employeeEditor.valid().is());
		assertFalse(employeeEditor.exists().is());
		// Master should be modified because detail has pending insert
		assertTrue(departmentEditor.modified().is());

		// masterExists=true, valid=false, exists=false -> all false (nothing to do)
		employeeEditor.value(Employee.SALARY).clear(); // now invalid
		assertFalse(employeeEditor.valid().is());
		assertFalse(employeeEditor.exists().is());
		assertFalse(departmentEditor.modified().is());
	}

	@Test
	void detailEditorInsertWithValidDetail() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			departmentEditor.value(Department.ID).set(50);
			departmentEditor.value(Department.NAME).set("NewDept");
			employeeEditor.value(Employee.NAME).set("NewEmp");
			employeeEditor.value(Employee.SALARY).set(3000d);

			insert(departmentEditor);

			assertTrue(departmentEditor.exists().is());
			assertTrue(employeeEditor.exists().is());
			// Detail entity has FK pointing to master
			assertEquals(1, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(50))));
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorInsertWithInvalidDetail() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			departmentEditor.value(Department.ID).set(51);
			departmentEditor.value(Department.NAME).set("DeptNoEmp");
			// Employee name set but salary null -> invalid
			employeeEditor.value(Employee.NAME).set("InvalidEmp");

			insert(departmentEditor);

			assertTrue(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());
			assertEquals(0, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(51))));
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorInsertWithEmptyDetail() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			departmentEditor.value(Department.ID).set(52);
			departmentEditor.value(Department.NAME).set("EmptyDetail");
			// No values set on detail -> not valid, not exists

			insert(departmentEditor);

			assertTrue(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());
			assertEquals(0, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(52))));
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateDeletePath() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master + detail
			departmentEditor.value(Department.ID).set(53);
			departmentEditor.value(Department.NAME).set("DelPath");
			employeeEditor.value(Employee.NAME).set("Emp");
			employeeEditor.value(Employee.SALARY).set(2000d);

			insert(departmentEditor);
			assertTrue(employeeEditor.exists().is());
			assertEquals(1, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(53))));

			// Make detail invalid -> triggers delete path on update
			employeeEditor.value(Employee.SALARY).clear();
			assertFalse(employeeEditor.valid().is());
			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);

			// Detail should be deleted
			assertFalse(employeeEditor.exists().is());
			assertEquals(0, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(53))));
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateInsertPath() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master only (detail invalid during insert)
			departmentEditor.value(Department.ID).set(54);
			departmentEditor.value(Department.NAME).set("InsPath");

			insert(departmentEditor);
			assertTrue(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());

			// Now set valid detail values
			employeeEditor.value(Employee.NAME).set("NewEmp");
			employeeEditor.value(Employee.SALARY).set(2500d);
			assertTrue(employeeEditor.valid().is());
			assertTrue(departmentEditor.modified().is());

			// Update master -> detail should be inserted
			update(departmentEditor);

			assertTrue(employeeEditor.exists().is());
			assertEquals(1, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(54))));
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateUpdatePath() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master + detail
			departmentEditor.value(Department.ID).set(55);
			departmentEditor.value(Department.NAME).set("UpdPath");
			employeeEditor.value(Employee.NAME).set("Emp");
			employeeEditor.value(Employee.SALARY).set(2000d);

			insert(departmentEditor);
			assertTrue(employeeEditor.exists().is());

			// Modify detail salary
			employeeEditor.value(Employee.SALARY).set(3000d);
			assertTrue(employeeEditor.modified().is());
			assertTrue(employeeEditor.valid().is());
			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);

			// Detail should be updated
			assertTrue(employeeEditor.exists().is());
			assertFalse(employeeEditor.modified().is());
			assertEquals(3000d, employeeEditor.value(Employee.SALARY).get());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateNoOp() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master + detail
			departmentEditor.value(Department.ID).set(56);
			departmentEditor.value(Department.NAME).set("NoOp");
			employeeEditor.value(Employee.NAME).set("Emp");
			employeeEditor.value(Employee.SALARY).set(2000d);

			insert(departmentEditor);
			assertTrue(employeeEditor.exists().is());

			// Modify only master, not detail
			departmentEditor.value(Department.NAME).set("NoOpMod");
			assertTrue(departmentEditor.modified().is());
			assertFalse(employeeEditor.modified().is());

			update(departmentEditor);

			// Detail unchanged
			assertTrue(employeeEditor.exists().is());
			assertFalse(employeeEditor.modified().is());
			assertEquals(2000d, employeeEditor.value(Employee.SALARY).get());
			// Verify DB row unchanged
			assertEquals(1, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(56))));
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateMasterUnmodifiedDetailModified() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master + detail
			departmentEditor.value(Department.ID).set(57);
			departmentEditor.value(Department.NAME).set("MasterUnmod");
			employeeEditor.value(Employee.NAME).set("Emp");
			employeeEditor.value(Employee.SALARY).set(2000d);

			insert(departmentEditor);

			// Modify only detail, not master
			employeeEditor.value(Employee.SALARY).set(4000d);
			assertTrue(employeeEditor.modified().is());
			// Master's modified() should be true via Updatable additional state
			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);

			// Detail should be updated
			assertTrue(employeeEditor.exists().is());
			assertEquals(4000d, employeeEditor.value(Employee.SALARY).get());
			assertFalse(employeeEditor.modified().is());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorDeleteWithExistingDetail() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master + detail
			departmentEditor.value(Department.ID).set(58);
			departmentEditor.value(Department.NAME).set("DelBoth");
			employeeEditor.value(Employee.NAME).set("Emp");
			employeeEditor.value(Employee.SALARY).set(2000d);

			insert(departmentEditor);
			assertTrue(departmentEditor.exists().is());
			assertTrue(employeeEditor.exists().is());

			delete(departmentEditor);

			// Both should be deleted and editors cleared
			assertFalse(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());
			assertEquals(0, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(58))));
			assertEquals(0, CONNECTION_PROVIDER.connection().count(
							Count.where(Department.ID.equalTo(58))));
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorDeleteWithNoDetail() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master only (detail invalid)
			departmentEditor.value(Department.ID).set(59);
			departmentEditor.value(Department.NAME).set("NoDet");

			insert(departmentEditor);
			assertTrue(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());

			// Delete master -> should not fail even though no detail exists
			delete(departmentEditor);

			assertFalse(departmentEditor.exists().is());
			assertEquals(0, CONNECTION_PROVIDER.connection().count(
							Count.where(Department.ID.equalTo(59))));
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorFullLifecycle() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// 1. Insert master + valid detail
			departmentEditor.value(Department.ID).set(60);
			departmentEditor.value(Department.NAME).set("Lifecycle");
			employeeEditor.value(Employee.NAME).set("Emp");
			employeeEditor.value(Employee.SALARY).set(2500d);

			insert(departmentEditor);
			assertTrue(departmentEditor.exists().is());
			assertTrue(employeeEditor.exists().is());

			// 2. Modify detail -> update master -> detail updated
			employeeEditor.value(Employee.SALARY).set(2000d);
			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);
			assertTrue(employeeEditor.exists().is());
			assertEquals(2000d, employeeEditor.value(Employee.SALARY).get());

			// 3. Invalidate detail -> update master -> detail deleted
			employeeEditor.value(Employee.SALARY).clear();
			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);
			assertFalse(employeeEditor.exists().is());
			assertEquals(0, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(60))));

			// 4. Set valid detail values -> update master -> detail re-inserted
			employeeEditor.value(Employee.NAME).set("EmpReborn");
			employeeEditor.value(Employee.SALARY).set(3000d);
			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);
			assertTrue(employeeEditor.exists().is());
			assertEquals(1, CONNECTION_PROVIDER.connection().count(
							Count.where(Employee.DEPARTMENT.equalTo(60))));

			// 5. Delete master -> both gone
			delete(departmentEditor);
			assertFalse(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());
			assertEquals(0, CONNECTION_PROVIDER.connection().count(
							Count.where(Department.ID.equalTo(60))));
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void multipleDetailEditors() throws EntityValidationException {
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());
			departmentEditor.detail().add(EditorLink.builder()
							.editor(extraEditor)
							.foreignKey(DepartmentExtra.DEPARTMENT_FK)
							.build());

			// Insert master with both details valid
			departmentEditor.value(Department.ID).set(61);
			departmentEditor.value(Department.NAME).set("Multi");
			employeeEditor.value(Employee.NAME).set("Emp");
			employeeEditor.value(Employee.SALARY).set(2000d);
			extraEditor.value(DepartmentExtra.DESCRIPTION).set("Extra info");

			insert(departmentEditor);
			assertTrue(departmentEditor.exists().is());
			assertTrue(employeeEditor.exists().is());
			assertTrue(extraEditor.exists().is());

			// Modify one detail (employee salary), invalidate other (extra description is nullable
			// so to invalidate we use the employee; let's modify extra and invalidate employee instead)
			extraEditor.value(DepartmentExtra.DESCRIPTION).set("Updated info");
			employeeEditor.value(Employee.SALARY).clear(); // invalidate employee

			assertTrue(departmentEditor.modified().is());

			update(departmentEditor);

			// Employee should be deleted (invalid), extra should be updated
			assertFalse(employeeEditor.exists().is());
			assertTrue(extraEditor.exists().is());
			assertEquals("Updated info", extraEditor.value(DepartmentExtra.DESCRIPTION).get());

			// Delete master -> all cleaned up
			delete(departmentEditor);
			assertFalse(departmentEditor.exists().is());
			assertFalse(employeeEditor.exists().is());
			assertFalse(extraEditor.exists().is());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void multiSlotDetailEditorNoShuffleAfterUpdate() throws EntityValidationException {
		// Two FK detail editors share the same FK, distinguished by an offset/limit-based DetailSelect.
		// After an update that re-orders the rows, slots must retain their original primary keys —
		// PK-stable post-persist reload, not loader-based reshuffle.
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			EntityConnection connection = CONNECTION_PROVIDER.connection();
			Entity dept40 = connection.selectSingle(Department.ID.equalTo(40));
			connection.insertSelect(CONNECTION_PROVIDER.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "ALPHA")
							.with(Employee.DEPARTMENT, 40)
							.with(Employee.SALARY, 2000d)
							.build());
			connection.insertSelect(CONNECTION_PROVIDER.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "BETA")
							.with(Employee.DEPARTMENT, 40)
							.with(Employee.SALARY, 3000d)
							.build());

			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor first = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor second = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);

			departmentEditor.detail().add(EditorLink.builder()
							.editor(first)
							.foreignKey(Employee.DEPARTMENT_FK)
							.name("first")
							.select(master -> Select.where(Employee.DEPARTMENT_FK.equalTo(master))
											.orderBy(ascending(Employee.NAME))
											.offset(0)
											.limit(1)
											.build())
							.present(EMPLOYEE_PRESENT)
							.build());
			departmentEditor.detail().add(EditorLink.builder()
							.editor(second)
							.foreignKey(Employee.DEPARTMENT_FK)
							.name("second")
							.select(master -> Select.where(Employee.DEPARTMENT_FK.equalTo(master))
											.orderBy(ascending(Employee.NAME))
											.offset(1)
											.limit(1)
											.build())
							.present(EMPLOYEE_PRESENT)
							.build());

			departmentEditor.entity().set(dept40);
			Entity.Key firstPk = first.entity().get().primaryKey();
			Entity.Key secondPk = second.entity().get().primaryKey();
			assertEquals("ALPHA", first.value(Employee.NAME).get());
			assertEquals("BETA", second.value(Employee.NAME).get());

			// Rename slot "second" so it sorts before slot "first" alphabetically.
			second.value(Employee.NAME).set("AAA");
			update(departmentEditor);

			// Slots retain their original primary keys despite the alphabetical reshuffle.
			assertEquals(firstPk, first.entity().get().primaryKey());
			assertEquals(secondPk, second.entity().get().primaryKey());
			assertEquals("ALPHA", first.value(Employee.NAME).get());
			assertEquals("AAA", second.value(Employee.NAME).get());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void multiSlotDetailEditorDeleteRetainsOtherSlot() throws EntityValidationException {
		// Multi-slot setup; deleting one slot must not cause the loader to slide the remaining
		// row from the surviving slot's offset into a different slot.
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			EntityConnection connection = CONNECTION_PROVIDER.connection();
			Entity dept40 = connection.selectSingle(Department.ID.equalTo(40));
			connection.insertSelect(CONNECTION_PROVIDER.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "ALPHA")
							.with(Employee.DEPARTMENT, 40)
							.with(Employee.SALARY, 2000d)
							.build());
			connection.insertSelect(CONNECTION_PROVIDER.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "BETA")
							.with(Employee.DEPARTMENT, 40)
							.with(Employee.SALARY, 3000d)
							.build());

			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor first = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor second = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);

			departmentEditor.detail().add(EditorLink.builder()
							.editor(first)
							.foreignKey(Employee.DEPARTMENT_FK)
							.name("first")
							.select(master -> Select.where(Employee.DEPARTMENT_FK.equalTo(master))
											.orderBy(ascending(Employee.NAME))
											.offset(0)
											.limit(1)
											.build())
							.present(EMPLOYEE_PRESENT)
							.build());
			departmentEditor.detail().add(EditorLink.builder()
							.editor(second)
							.foreignKey(Employee.DEPARTMENT_FK)
							.name("second")
							.select(master -> Select.where(Employee.DEPARTMENT_FK.equalTo(master))
											.orderBy(ascending(Employee.NAME))
											.offset(1)
											.limit(1)
											.build())
							.present(EMPLOYEE_PRESENT)
							.build());

			departmentEditor.entity().set(dept40);
			Entity.Key secondPk = second.entity().get().primaryKey();
			assertTrue(first.exists().is());
			assertTrue(second.exists().is());

			// Trigger delete on slot "first" by clearing its salary -> present=false, exists=true.
			first.value(Employee.SALARY).clear();
			update(departmentEditor);

			assertFalse(first.exists().is());
			// Slot "second" must still hold BETA — without PK-stable, the loader would shift
			// BETA into slot "first" (offset 0) and leave slot "second" empty.
			assertEquals(secondPk, second.entity().get().primaryKey());
			assertEquals("BETA", second.value(Employee.NAME).get());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorReplaceAfterMasterUpdatePreservesSlot() throws EntityValidationException {
		// FK detail with an additional load condition. Editing the row to no longer satisfy that
		// condition leaves it in the slot post-persist — the master's primary key is unchanged so
		// replace() doesn't trigger a reload, and the participating detail's own persist task
		// replaces its entity with the freshly-updated row. The next master selection re-runs the
		// loader and correctly empties the slot.
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor highEarnerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);

			departmentEditor.detail().add(EditorLink.builder()
							.editor(highEarnerEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.condition(master -> and(
											Employee.DEPARTMENT_FK.equalTo(master),
											Employee.SALARY.greaterThanOrEqualTo(3000d)))
							.present(EMPLOYEE_PRESENT)
							.build());

			departmentEditor.value(Department.ID).set(50);
			departmentEditor.value(Department.NAME).set("FK+Cond");
			highEarnerEditor.value(Employee.NAME).set("RICH");
			highEarnerEditor.value(Employee.SALARY).set(3000d);

			insert(departmentEditor);
			Entity.Key highEarnerPk = highEarnerEditor.entity().get().primaryKey();
			assertTrue(highEarnerEditor.exists().is());

			// Edit salary to no longer satisfy the load condition.
			highEarnerEditor.value(Employee.SALARY).set(2500d);
			update(departmentEditor);

			// Same-PK replace doesn't reload; the slot retains the just-edited row even though
			// it no longer satisfies the load condition.
			assertEquals(highEarnerPk, highEarnerEditor.entity().get().primaryKey());
			assertEquals(2500d, highEarnerEditor.value(Employee.SALARY).get());

			// Master selection re-runs the loader; condition no longer matches -> slot empties.
			Entity dept50 = CONNECTION_PROVIDER.connection().selectSingle(Department.ID.equalTo(50));
			departmentEditor.entity().set(null);
			departmentEditor.entity().set(dept50);
			assertFalse(highEarnerEditor.exists().is());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	@Test
	void detailEditorReplaceWithDifferentMasterReloadsDetail() {
		// replace() with a different master primary key reloads details via their loaders —
		// otherwise slots would hold the previous master's children. Same-PK replace remains a
		// silent value refresh that does not trigger a reload.
		CONNECTION_PROVIDER.connection().startTransaction();
		try {
			EntityConnection connection = CONNECTION_PROVIDER.connection();
			Entity deptA = connection.insertSelect(CONNECTION_PROVIDER.entities().entity(Department.TYPE)
							.with(Department.ID, 71)
							.with(Department.NAME, "DEPT_A")
							.build());
			Entity empA = connection.insertSelect(CONNECTION_PROVIDER.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "EMP_A")
							.with(Employee.DEPARTMENT, 71)
							.with(Employee.SALARY, 2000d)
							.build());
			Entity deptB = connection.insertSelect(CONNECTION_PROVIDER.entities().entity(Department.TYPE)
							.with(Department.ID, 72)
							.with(Department.NAME, "DEPT_B")
							.build());
			Entity empB = connection.insertSelect(CONNECTION_PROVIDER.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "EMP_B")
							.with(Employee.DEPARTMENT, 72)
							.with(Employee.SALARY, 2000d)
							.build());

			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION_PROVIDER);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION_PROVIDER);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Master selection loads dept A's employee.
			departmentEditor.entity().set(deptA);
			assertEquals(empA.primaryKey(), employeeEditor.entity().get().primaryKey());

			// replace() with a different master must reload the detail.
			departmentEditor.entity().replace(deptB);
			assertEquals(empB.primaryKey(), employeeEditor.entity().get().primaryKey());

			// replace() with same PK doesn't reload — mutate the slot's in-memory name and confirm
			// a same-PK replace leaves the mutation intact.
			employeeEditor.value(Employee.NAME).set("MUTATED");
			Entity deptBRefreshed = connection.selectSingle(Department.ID.equalTo(72));
			departmentEditor.entity().replace(deptBRefreshed);
			assertEquals("MUTATED", employeeEditor.value(Employee.NAME).get());

			// replace(null) clears details (identity goes from real -> null).
			departmentEditor.entity().replace(null);
			assertFalse(employeeEditor.exists().is());
		}
		finally {
			CONNECTION_PROVIDER.connection().rollbackTransaction();
		}
	}

	private static void insert(TestEntityEditor editor) throws EntityValidationException {
		editor.tasks(editor.connectionProvider().connection()).insert().perform().handle();
	}

	private static void update(TestEntityEditor editor) throws EntityValidationException {
		editor.tasks(editor.connectionProvider().connection()).update().perform().handle();
	}

	private static void delete(TestEntityEditor editor) {
		editor.tasks(editor.connectionProvider().connection()).delete().perform().handle();
	}

	private static final class TestEntityEditor extends DefaultEntityEditor<TestEntityEditor> {

		private TestEntityEditor(EntityType entityType, EntityConnectionProvider connectionProvider) {
			super(entityType, connectionProvider, new TestComponentModels());
		}
	}

	private static final class TestComponentModels implements EntityEditor.ComponentModels {}

	private static final class TestEntityModel extends DefaultEntityModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel, TestEntityEditor> {
		private TestEntityModel(TestEntityEditModel editModel) {
			super(editModel);
		}
	}

	private static final class TestEntityEditModel extends DefaultEntityEditModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel, TestEntityEditor> {

		private TestEntityEditModel(EntityType entityType, EntityConnectionProvider connectionProvider) {
			super(new TestEntityEditor(entityType, connectionProvider));
		}
	}

	private interface TestEntityTableModel extends EntityTableModel<TestEntityModel, TestEntityEditModel, TestEntityTableModel, TestEntityEditor> {}
}
