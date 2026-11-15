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

import is.codion.common.model.component.combobox.FilterComboBoxModel;
import is.codion.common.reactive.observable.Change;
import is.codion.common.reactive.observable.Observable;
import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.reactive.value.Value;
import is.codion.common.utilities.user.User;
import is.codion.framework.db.EntityConnection;
import is.codion.framework.db.EntityConnection.Count;
import is.codion.framework.db.EntityConnection.Select;
import is.codion.framework.db.local.LocalEntityConnection;
import is.codion.framework.domain.entity.Entities;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.EntityValidator;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.framework.domain.entity.exception.AttributeValidationException;
import is.codion.framework.domain.entity.exception.EntityValidationException;
import is.codion.framework.model.AbstractEntityEditor.DefaultDetailEditors;
import is.codion.framework.model.AbstractEntityEditor.DetailForeignKeyValidator;
import is.codion.framework.model.DetailDomain.Assignment;
import is.codion.framework.model.DetailDomain.Department;
import is.codion.framework.model.DetailDomain.DepartmentExtra;
import is.codion.framework.model.DetailDomain.DepartmentView;
import is.codion.framework.model.DetailDomain.Employee;
import is.codion.framework.model.DetailDomain.LazyEmployee;
import is.codion.framework.model.DetailDomain.SharedEmployee;
import is.codion.framework.model.DetailPersistenceFactory.DepartmentPersistence;
import is.codion.framework.model.EntityEditor.EditorTask.Result;
import is.codion.framework.model.EntityEditor.EditorValue;
import is.codion.framework.model.LocationDomain.Continent;
import is.codion.framework.model.LocationDomain.Country;
import is.codion.framework.model.LocationDomain.Sighting;
import is.codion.framework.model.LocationDomain.Specimen;

import org.junit.jupiter.api.Test;

import java.lang.ref.WeakReference;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static is.codion.framework.domain.entity.OrderBy.ascending;
import static is.codion.framework.domain.entity.condition.Condition.and;
import static is.codion.framework.model.PersistenceEvents.persistenceEvents;
import static java.util.Arrays.asList;
import static java.util.Collections.*;
import static org.junit.jupiter.api.Assertions.*;

public final class AbstractEntityEditorTest {

	private static final User UNIT_TEST_USER =
					User.parse(System.getProperty("codion.test.user", "scott:tiger"));

	private static final EntityConnection CONNECTION = LocalEntityConnection.builder()
					.domain(new DetailDomain())
					.user(UNIT_TEST_USER)
					.build();

	private static final EntityConnection LOCATION_CONNECTION = LocalEntityConnection.builder()
					.domain(new LocationDomain())
					.user(UNIT_TEST_USER)
					.build();

	private static final Predicate<Entity> EMPLOYEE_PRESENT = employee ->
					employee.present(Employee.SALARY) && employee.present(Employee.NAME);

	@Test
	void warningsAreSoft() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		EditorValue<Double> salary = editor.value(Employee.SALARY);
		// Asked for before the validator that warns is installed, so this also covers the refresh a new validator triggers.
		assertFalse(salary.warned().is());
		assertNull(salary.warning().get());

		editor.validator().set(new SalaryValidator());

		salary.set(2_500d);
		assertFalse(salary.warned().is());

		salary.set(8_000d);
		assertTrue(salary.warned().is());
		assertEquals("Unusually high", salary.warning().get());

		// Soft means soft. 8000 is within the attribute's own 2000-10000 range, so the hard tier has nothing to say and
		// says nothing: the value stays valid and validation passes, warned or not.
		assertTrue(salary.valid().is());
		assertNull(salary.error().get());
		assertDoesNotThrow(() -> editor.validate(Employee.SALARY));

		salary.set(2_500d);
		assertFalse(salary.warned().is());
		assertNull(salary.warning().get());
	}

	@Test
	void aWarningFollowsTheValuesItDependsOn() {
		// The reason warnings are refreshed wholesale rather than per changed attribute: this one is about the manager,
		// but what makes it odd is the salary. Moving the salary has to move the manager's warning, untouched though it is.
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.validator().set(new SalaryValidator());
		EditorValue<Integer> manager = editor.value(Employee.MANAGER_ID);

		editor.value(Employee.SALARY).set(2_500d);
		assertFalse(manager.warned().is());

		editor.value(Employee.SALARY).set(8_000d);
		assertTrue(manager.warned().is());
		assertEquals("A salary this high usually reports to someone", manager.warning().get());

		manager.set(8);
		assertFalse(manager.warned().is());
	}

	@Test
	void warningsApplyToAnUnmodifiedValueOfAnExistingEntity() {
		// Errors do not - validation skips unmodified values when updating, so that opening a record does not spray
		// complaints about fields nobody has touched. A warning is the opposite: flagging an odd value on the record you
		// just opened is the review case the tier exists for. KING earns 5000 and reports to nobody, both untouched here.
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.validator().set(new SalaryValidator());
		editor.entity().set(CONNECTION.selectSingle(Employee.NAME.equalTo("KING")));

		EditorValue<Double> salary = editor.value(Employee.SALARY);
		assertTrue(editor.entity().exists().is());
		assertFalse(salary.modified().is());
		assertTrue(salary.warned().is());
		assertTrue(editor.value(Employee.MANAGER_ID).warned().is());
	}

	private static final class SalaryValidator implements EntityValidator {

		@Override
		public Optional<String> warning(Entity employee, Attribute<?> attribute) {
			Double salary = employee.get(Employee.SALARY);
			if (attribute.equals(Employee.SALARY) && salary != null && salary > 3_000d) {
				return Optional.of("Unusually high");
			}
			if (attribute.equals(Employee.MANAGER_ID) && salary != null && salary > 3_000d
							&& !employee.present(Employee.MANAGER_ID)) {
				return Optional.of("A salary this high usually reports to someone");
			}

			return Optional.empty();
		}
	}

	@Test
	void clearSetsNull() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		departmentEditor.entity().clear();
		assertTrue(departmentEditor.entity().get().contains(Department.ID));
		assertTrue(departmentEditor.entity().get().contains(Department.NAME));

		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		employeeEditor.entity().clear();
		assertTrue(employeeEditor.entity().get().contains(Employee.ID));
		assertTrue(employeeEditor.entity().get().contains(Employee.NAME));
		assertTrue(employeeEditor.entity().get().contains(Employee.SALARY));
		assertTrue(employeeEditor.entity().get().contains(Employee.MANAGER_ID));
	}

	@Test
	void defaultsAndClearNotifyTheEntityHeld() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		List<Entity> notified = new ArrayList<>();
		editor.entity().addConsumer(notified::add);
		// the entity held, as get() returns it, never null
		editor.entity().defaults();
		assertNotNull(notified.get(0));
		assertTrue(notified.get(0).valuesEqual(editor.entity().get()));
		editor.entity().clear();
		assertNotNull(notified.get(1));
		assertTrue(notified.get(1).valuesEqual(editor.entity().get()));
		// so changed() reports the actual change, the defaulted entity in between
		Entity king = CONNECTION.selectSingle(Employee.NAME.equalTo("KING"));
		List<Change<Entity>> changes = new ArrayList<>();
		editor.entity().changed().addConsumer(changes::add);
		editor.entity().set(king);
		editor.entity().defaults();
		editor.entity().set(king);
		assertEquals(3, changes.size());
		assertEquals(king, changes.get(1).previous());
		assertNotNull(changes.get(1).current());
		assertFalse(changes.get(1).current().primaryKey().present());
		assertNotNull(changes.get(2).previous());
		assertFalse(changes.get(2).previous().primaryKey().present());
		assertEquals(king, changes.get(2).current());
	}

	@Test
	void modifiedAttributesExcludeForeignKeyColumns() {
		Entity king = CONNECTION.selectSingle(Employee.NAME.equalTo("KING"));
		Entity research = CONNECTION.selectSingle(Department.ID.equalTo(20));
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		// the department column with a value of its own, bound to a component for example
		editor.value(Employee.DEPARTMENT);
		editor.entity().set(king);
		editor.value(Employee.DEPARTMENT_FK).set(research);
		// the column reported via its foreign key
		assertEquals(singleton(Employee.DEPARTMENT_FK), editor.entity().modified().attributes().get());

		// the column alone, its foreign key without a value
		TestEntityEditor columnEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		columnEditor.entity().set(king);
		columnEditor.value(Employee.DEPARTMENT).set(20);
		assertEquals(singleton(Employee.DEPARTMENT), columnEditor.entity().modified().attributes().get());
	}

	@Test
	void revertRestoresForeignKeys() {
		Entity king = CONNECTION.selectSingle(Employee.NAME.equalTo("KING"));
		Entity research = CONNECTION.selectSingle(Department.ID.equalTo(20));
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.entity().set(king);
		editor.value(Employee.DEPARTMENT_FK).set(research);
		editor.entity().revert();
		// the department restored, the column defined before its foreign key restored along with it
		Entity department = editor.value(Employee.DEPARTMENT_FK).get();
		assertNotNull(department);
		assertEquals(king.entity(Employee.DEPARTMENT_FK), department);
		assertEquals(king.entity(Employee.DEPARTMENT_FK).get(Department.NAME), department.get(Department.NAME));
		assertEquals(king.get(Employee.DEPARTMENT), editor.value(Employee.DEPARTMENT).get());
		assertFalse(editor.entity().modified().is());
		// so editing it again modifies it
		editor.value(Employee.DEPARTMENT_FK).set(research);
		assertTrue(editor.value(Employee.DEPARTMENT_FK).modified().is());
		assertEquals(singleton(Employee.DEPARTMENT_FK), editor.entity().modified().attributes().get());
	}

	@Test
	void setIgnoresComponentEdits() {
		// the continent notified first, the country second, and the other way around
		setIgnoresComponentEdits(Specimen.TYPE, Specimen.ID, Specimen.CONTINENT_FK, Specimen.COUNTRY_FK);
		setIgnoresComponentEdits(Sighting.TYPE, Sighting.ID, Sighting.CONTINENT_FK, Sighting.COUNTRY_FK);
	}

	@Test
	void setIgnoresConsumerEdits() {
		// the country notified after the continent, and before it
		setIgnoresConsumerEdits(Specimen.TYPE, Specimen.ID, Specimen.CONTINENT_FK, Specimen.COUNTRY_FK);
		setIgnoresConsumerEdits(Sighting.TYPE, Sighting.ID, Sighting.CONTINENT_FK, Sighting.COUNTRY_FK);
	}

	@Test
	void setNotifiesInAttributeOrder() {
		TestEntityEditor editor = new TestEntityEditor(Sighting.TYPE, LOCATION_CONNECTION);
		List<Attribute<?>> notified = new ArrayList<>();
		editor.values().changed().addConsumer(notified::add);
		Entities entities = LOCATION_CONNECTION.entities();
		Entity europe = entities.entity(Continent.TYPE)
						.with(Continent.CODE, "EU")
						.build();
		editor.entity().set(entities.entity(Sighting.TYPE)
						.with(Sighting.ID, 1)
						.with(Sighting.CONTINENT_FK, europe)
						.with(Sighting.COUNTRY_FK, entities.entity(Country.TYPE)
										.with(Country.CODE, "IS")
										.with(Country.CONTINENT_FK, europe)
										.build())
						.build());
		assertEquals(asList(Sighting.ID, Sighting.COUNTRY_CODE, Sighting.COUNTRY_FK,
						Sighting.CONTINENT_CODE, Sighting.CONTINENT_FK), notified);
	}

	/**
	 * A country combo box filtered by the continent combo box, clearing a selected country excluded by the filter,
	 * each bound to the editor as an edit panel binds them, via their selection.
	 */
	private static void setIgnoresComponentEdits(EntityType entityType, Column<Integer> id,
																							 ForeignKey continentFk, ForeignKey countryFk) {
		Entities entities = LOCATION_CONNECTION.entities();
		Entity southAmerica = entities.entity(Continent.TYPE)
						.with(Continent.CODE, "SA")
						.build();
		Entity oceania = entities.entity(Continent.TYPE)
						.with(Continent.CODE, "OC")
						.build();
		Entity brazil = entities.entity(Country.TYPE)
						.with(Country.CODE, "BR")
						.with(Country.CONTINENT_FK, southAmerica)
						.build();
		Entity australia = entities.entity(Country.TYPE)
						.with(Country.CODE, "AU")
						.with(Country.CONTINENT_FK, oceania)
						.build();

		EntityComboBoxModel continents = EntityComboBoxModel.builder()
						.entityType(Continent.TYPE)
						.connection(LOCATION_CONNECTION)
						.refresh(false)
						.build();
		continents.items().set(asList(southAmerica, oceania));
		EntityComboBoxModel countries = EntityComboBoxModel.builder()
						.entityType(Country.TYPE)
						.connection(LOCATION_CONNECTION)
						.refresh(false)
						.filterSelected(true)
						.build();
		countries.items().set(asList(brazil, australia));
		countries.filter().get(Country.CONTINENT_FK).link(continents);

		TestEntityEditor editor = new TestEntityEditor(entityType, LOCATION_CONNECTION);
		continents.selection().item().link(editor.value(continentFk));
		countries.selection().item().link(editor.value(countryFk));
		List<Entity> edited = new ArrayList<>();
		editor.value(countryFk).edited().addConsumer(edited::add);

		Entity inBrazil = entities.entity(entityType)
						.with(id, 1)
						.with(continentFk, southAmerica)
						.with(countryFk, brazil)
						.build();
		Entity inAustralia = entities.entity(entityType)
						.with(id, 2)
						.with(continentFk, oceania)
						.with(countryFk, australia)
						.build();
		for (Entity entity : asList(inBrazil, inAustralia, inBrazil)) {
			editor.entity().set(entity);
			// the country left over from the previous entity, cleared by the filter, does not replace the one set
			assertFalse(editor.entity().modified().is());
			assertEquals(entity.get(countryFk), editor.value(countryFk).get());
			assertEquals(entity.get(countryFk), countries.selection().item().get());
			assertEquals(entity.get(continentFk), continents.selection().item().get());
		}
		assertTrue(edited.isEmpty());
	}

	/**
	 * A consumer of the continent clearing the country, the country changed by the entity set and unchanged.
	 */
	private static void setIgnoresConsumerEdits(EntityType entityType, Column<Integer> id,
																							ForeignKey continentFk, ForeignKey countryFk) {
		Entities entities = LOCATION_CONNECTION.entities();
		Entity europe = entities.entity(Continent.TYPE)
						.with(Continent.CODE, "EU")
						.build();
		Entity asia = entities.entity(Continent.TYPE)
						.with(Continent.CODE, "AS")
						.build();
		Entity iceland = entities.entity(Country.TYPE)
						.with(Country.CODE, "IS")
						.with(Country.CONTINENT_FK, europe)
						.build();

		TestEntityEditor editor = new TestEntityEditor(entityType, LOCATION_CONNECTION);
		editor.value(continentFk).addConsumer(continent -> editor.value(countryFk).clear());

		editor.entity().set(entities.entity(entityType)
						.with(id, 1)
						.with(continentFk, europe)
						.with(countryFk, iceland)
						.build());
		assertFalse(editor.entity().modified().is());
		assertEquals(iceland, editor.value(countryFk).get());
		editor.entity().set(entities.entity(entityType)
						.with(id, 2)
						.with(continentFk, asia)
						.with(countryFk, iceland)
						.build());
		assertFalse(editor.entity().modified().is());
		assertEquals(iceland, editor.value(countryFk).get());
		// an edit still notifies the consumer, which then edits the country
		editor.value(continentFk).set(europe);
		assertTrue(editor.value(countryFk).isNull());
		assertTrue(editor.entity().modified().is());
	}

	@Test
	void simpleDetailEditor() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(departmentEditor.entity().exists().is());
			assertTrue(employeeEditor.entity().exists().is());

			salary.clear();//now invalid
			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);
			assertFalse(employeeEditor.entity().exists().is());

			employeeName.set("Test");
			salary.set(2100d);// valid again

			update(departmentEditor);
			assertTrue(employeeEditor.entity().exists().is());

			salary.set(1990d);// present but invalid
			assertThrows(EntityValidationException.class, () -> insert(departmentEditor));

			delete(departmentEditor);
			assertFalse(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void threeLevelDetailEditor() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());
			TestEntityEditor managerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(departmentEditor.entity().exists().is());
			assertTrue(employeeEditor.entity().exists().is());
			assertTrue(managerEditor.entity().exists().is());

			employeeSalary.clear();//now invalid
			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);
			assertFalse(employeeEditor.entity().exists().is());
			assertFalse(managerEditor.entity().exists().is());

			employeeName.set("Test");
			employeeSalary.set(2100d);// valid again

			update(departmentEditor);
			assertTrue(employeeEditor.entity().exists().is());
			assertFalse(managerEditor.entity().exists().is());

			managerName.set("TestMgr2");
			managerSalary.set(3100d);// valid again
			managerDepartment.set(10);

			assertTrue(employeeEditor.entity().modified().is());
			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);
			assertTrue(employeeEditor.entity().exists().is());
			assertTrue(managerEditor.entity().exists().is());

			delete(departmentEditor);
			assertFalse(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());
			assertFalse(managerEditor.entity().exists().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorSetup() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
		assertFalse(employeeEditor.entity().exists().is());
		assertFalse(employeeEditor.entity().modified().is());
		assertNull(employeeEditor.value(Employee.NAME).get());
		assertNull(employeeEditor.value(Employee.SALARY).get());
		// Master's modified().additional() contains the detail's updatable state
		assertFalse(departmentEditor.entity().modified().additional().get().isEmpty());
		// The three framework-managed declarations are locked, all three rejecting with the
		// IllegalArgumentException a Value validator is contracted to throw
		assertThrows(IllegalArgumentException.class, () -> employeeEditor.validator().set(new EntityValidator() {}));
		assertThrows(IllegalArgumentException.class, () -> employeeEditor.entity().present().predicate().set(entity -> true));
		assertThrows(IllegalArgumentException.class, () -> employeeEditor.value(Employee.DEPARTMENT_FK).persist().set(true));
	}

	@Test
	void detailEditorModifiedAttributesStayFresh() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(departmentEditor.entity().modified().attributes().get().contains(Employee.NAME));
			assertTrue(departmentEditor.entity().modified().attributes().get().contains(Employee.SALARY));

			// Revert NAME back to original, keep SALARY modified. The detail's Updatable.update
			// state stays true (no flip — still present, exists, and modified via SALARY), but
			// the modified-attributes set must refresh to drop NAME.
			employeeEditor.value(Employee.NAME).set("Original");
			assertFalse(departmentEditor.entity().modified().attributes().get().contains(Employee.NAME));
			assertTrue(departmentEditor.entity().modified().attributes().get().contains(Employee.SALARY));

			delete(departmentEditor);
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorValidReflectsContext() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		// Present iff NAME is set — lets us reach a "present, but SALARY (a required non-FK field) missing" state.
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(employee -> employee.present(Employee.NAME))
						.build());

		// Not present (NAME unset): the detail will not be created, so it is valid despite its required fields being empty.
		assertFalse(employeeEditor.entity().present().is());
		assertTrue(employeeEditor.entity().valid().is());

		// Present (NAME set) but SALARY (a required non-FK field) still missing -> invalid: a present detail validates.
		employeeEditor.value(Employee.NAME).set("Test");
		assertTrue(employeeEditor.entity().present().is());
		assertFalse(employeeEditor.entity().valid().is());

		// SALARY set too -> present and valid: the master FK is null but framework-managed, so exempt.
		employeeEditor.value(Employee.SALARY).set(2000d);
		assertTrue(employeeEditor.entity().valid().is());

		// Clearing NAME makes it not present again -> valid, even though SALARY alone leaves the required NAME empty.
		employeeEditor.value(Employee.NAME).clear();
		assertFalse(employeeEditor.entity().present().is());
		assertTrue(employeeEditor.entity().valid().is());
	}

	@Test
	void nonForeignKeyDetailEditor() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);

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
			assertTrue(departmentEditor.entity().exists().is());
			assertTrue(employeeEditor.entity().exists().is());
			assertEquals(1, beforeInsertCalls.get());
			assertEquals(61, employeeEditor.entity().get().get(Employee.DEPARTMENT));

			// Master changes -> detail loads via user-supplied condition
			EntityConnection connection = CONNECTION;
			Entity dept62 = connection.insertSelect(
							CONNECTION.entities().entity(Department.TYPE)
											.with(Department.ID, 62)
											.with(Department.NAME, "Empty")
											.build());
			departmentEditor.entity().set(dept62);
			assertFalse(employeeEditor.entity().exists().is());

			Entity dept61 = connection.selectSingle(Department.ID.equalTo(61));
			departmentEditor.entity().set(dept61);
			assertTrue(employeeEditor.entity().exists().is());
			assertEquals("Solo", employeeEditor.value(Employee.NAME).get());

			// Update path
			employeeEditor.value(Employee.SALARY).set(2700d);
			update(departmentEditor);
			assertEquals(2700d, connection.selectSingle(Employee.NAME.equalTo("Solo")).get(Employee.SALARY));

			// Delete cascades to detail
			delete(departmentEditor);
			assertFalse(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void foreignKeyDetailEditorNameOverride() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor managerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		TestEntityEditor headEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);

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
		TestEntityEditor employee = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor managerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
		assertThrows(IllegalArgumentException.class,
						() -> detail.get(DepartmentExtra.DEPARTMENT_FK));
		assertThrows(IllegalArgumentException.class,
						() -> detail.name(DepartmentExtra.DEPARTMENT_FK));
	}

	@Test
	void detailEditorsWithSameNameButDifferentForeignKeys() {
		// Two FK-based detail editors with the same explicit name but different foreign keys
		// should coexist — the (FK, name) tuple distinguishes them. Motivated by a copy-paste
		// scenario where three FK links were given the same .name() by accident.
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);

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
		TestEntityEditor duplicateEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		assertThrows(IllegalArgumentException.class, () ->
						departmentEditor.detail().add(EditorLink.builder()
										.editor(duplicateEditor)
										.foreignKey(Employee.DEPARTMENT_FK)
										.name("X")
										.build()));
	}

	@Test
	void editorLinkCaption() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor managerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		TestEntityEditor headEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		TestEntityEditor plainEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);

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
		assertThrows(IllegalArgumentException.class,
						() -> detail.caption("unknown"));

		// Empty/null caption rejected
		TestEntityEditor employee = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
		TestEntityEditor otherDept = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor otherEmp = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);

		// Wrong detail entity type: DepartmentExtra editor with Employee foreign key
		assertThrows(IllegalArgumentException.class, () ->
						departmentEditor.detail().add(EditorLink.builder()
										.editor(extraEditor)
										.foreignKey(Employee.DEPARTMENT_FK)
										.build()));
		// Wrong master entity type: Employee FK references Department, but using employee as master
		TestEntityEditor anotherEmployeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		assertThrows(IllegalArgumentException.class, () ->
						anotherEmployeeEditor.detail().add(EditorLink.builder()
										.editor(employeeEditor)
										.foreignKey(Employee.DEPARTMENT_FK)
										.build()));
	}

	@Test
	void detailEditorMasterChanged() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(employeeEditor.entity().exists().is());

			// Insert another department
			EntityConnection connection = CONNECTION;
			Entity dept43 = connection.insertSelect(
							CONNECTION.entities().entity(Department.TYPE)
											.with(Department.ID, 43)
											.with(Department.NAME, "Other")
											.build());

			// Set a different entity on master -> detail should load from DB (no match for dept 43)
			departmentEditor.entity().set(dept43);
			assertFalse(employeeEditor.entity().exists().is());

			// Set master back to original dept -> detail loads from DB
			Entity dept42 = connection.selectSingle(Department.ID.equalTo(42));
			departmentEditor.entity().set(dept42);
			assertTrue(employeeEditor.entity().exists().is());
			assertEquals("Emp1", employeeEditor.value(Employee.NAME).get());

			// Set master entity to null -> detail is cleared
			departmentEditor.entity().defaults();
			assertFalse(employeeEditor.entity().exists().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdatableStates() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build());

		// masterExists=false -> master modified is false regardless
		assertFalse(departmentEditor.entity().exists().is());
		employeeEditor.value(Employee.NAME).set("Test");
		employeeEditor.value(Employee.SALARY).set(1000d);
		assertFalse(departmentEditor.entity().modified().is());

		// Simulate master existing by setting an existing entity (dept 40 has no employees)
		Entity existingDept = CONNECTION
						.selectSingle(Department.ID.equalTo(40));
		departmentEditor.entity().set(existingDept);
		assertTrue(departmentEditor.entity().exists().is());

		// masterExists=true, valid=true, exists=false -> insert state (detail is new and valid)
		// The employee editor was cleared by masterChanged (dept 40 has no detail employee)
		employeeEditor.value(Employee.NAME).set("Test");
		employeeEditor.value(Employee.SALARY).set(2000d);
		assertTrue(employeeEditor.entity().valid().is());
		assertFalse(employeeEditor.entity().exists().is());
		// Master should be modified because detail has pending insert
		assertTrue(departmentEditor.entity().modified().is());

		// Clearing SALARY makes the detail not present (EMPLOYEE_PRESENT needs NAME && SALARY): a detail that will not be
		// created is valid and has nothing to do -> not existing, master not modified.
		employeeEditor.value(Employee.SALARY).clear();
		assertFalse(employeeEditor.entity().present().is());
		assertTrue(employeeEditor.entity().valid().is());
		assertFalse(employeeEditor.entity().exists().is());
		assertFalse(departmentEditor.entity().modified().is());
	}

	@Test
	void detailEditorInsertWithValidDetail() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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

			assertTrue(departmentEditor.entity().exists().is());
			assertTrue(employeeEditor.entity().exists().is());
			// Detail entity has FK pointing to master
			assertEquals(1, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(50))));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorInsertWithInvalidDetail() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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

			assertTrue(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());
			assertEquals(0, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(51))));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailVerifiedBeforeMasterInsertEvent() {
		// the master's before insert event fired before its details were validated, for an insert then refused
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		List<Collection<Entity>> before = beforeEvents(departmentEditor, employeeEditor);
		departmentEditor.value(Department.ID).set(77);
		departmentEditor.value(Department.NAME).set("DEPT");
		employeeEditor.value(Employee.NAME).set("EMP");
		employeeEditor.value(Employee.SALARY).set(1990d);// present, below the range
		assertTrue(departmentEditor.entity().valid().is());
		assertThrows(EntityValidationException.class, departmentEditor.tasks()::insert);
		assertTrue(before.isEmpty());
	}

	@Test
	void detailVerifiedBeforeMasterUpdateEvent() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
			List<Collection<Entity>> before = beforeEvents(departmentEditor, employeeEditor);
			departmentEditor.value(Department.ID).set(77);
			departmentEditor.value(Department.NAME).set("DEPT");
			employeeEditor.value(Employee.NAME).set("EMP");
			employeeEditor.value(Employee.SALARY).set(2000d);
			insert(departmentEditor);
			assertTrue(employeeEditor.entity().exists().is());
			before.clear();
			departmentEditor.value(Department.NAME).set("RENAMED");
			employeeEditor.value(Employee.SALARY).set(1990d);
			assertThrows(EntityValidationException.class, departmentEditor.tasks()::update);
			assertTrue(before.isEmpty());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailVerifiedBeforeMasterDeleteEvent() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
			List<Collection<Entity>> before = beforeEvents(departmentEditor, employeeEditor);
			departmentEditor.value(Department.ID).set(77);
			departmentEditor.value(Department.NAME).set("DEPT");
			employeeEditor.value(Employee.NAME).set("EMP");
			employeeEditor.value(Employee.SALARY).set(2000d);
			insert(departmentEditor);
			assertTrue(employeeEditor.entity().exists().is());
			before.clear();
			employeeEditor.settings().deleteEnabled().set(false);
			assertThrows(IllegalStateException.class, departmentEditor.tasks()::delete);
			assertTrue(before.isEmpty());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	/**
	 * Links the employee editor to the department editor, collecting the before events of both
	 */
	private static List<Collection<Entity>> beforeEvents(TestEntityEditor departmentEditor, TestEntityEditor employeeEditor) {
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build());
		List<Collection<Entity>> before = new ArrayList<>();
		for (TestEntityEditor editor : asList(departmentEditor, employeeEditor)) {
			editor.events().before().insert().addConsumer(before::add);
			editor.events().before().update().addConsumer(before::add);
			editor.events().before().delete().addConsumer(before::add);
		}

		return before;
	}

	@Test
	void persistResultLeavesMovedOnEditorAlone() throws EntityValidationException {
		// the result of a persist task replaced, or reset, the entity of an editor set to another one since the task
		// was created, after notifying the other
		CONNECTION.startTransaction();
		try {
			TestEntityEditor editor = new TestEntityEditor(Department.TYPE, CONNECTION);
			List<Object> after = new ArrayList<>();
			editor.events().after().insert().addConsumer(after::add);
			editor.events().after().update().addConsumer(after::add);
			editor.events().after().delete().addConsumer(after::add);
			Entity ten = CONNECTION.selectSingle(Department.ID.equalTo(10));

			// update
			editor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(40)));
			editor.value(Department.NAME).set("RENAMED");
			Result<Entity> result = editor.tasks().update().perform();
			editor.entity().set(ten);
			result.handle();
			assertEquals(10, editor.value(Department.ID).get());
			assertEquals(ten.get(Department.NAME), editor.value(Department.NAME).get());
			assertEquals(1, after.size());

			// insert
			editor.entity().defaults();
			editor.value(Department.ID).set(77);
			editor.value(Department.NAME).set("NEW");
			result = editor.tasks().insert().perform();
			editor.entity().set(ten);
			result.handle();
			assertEquals(10, editor.value(Department.ID).get());
			assertEquals(2, after.size());

			// delete, of the department just inserted
			editor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(77)));
			result = editor.tasks().delete().perform();
			editor.entity().set(ten);
			result.handle();
			assertEquals(10, editor.value(Department.ID).get());
			assertEquals(3, after.size());

			// an edit is not moving on, the result applied over it
			editor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(40)));
			editor.value(Department.NAME).set("FIRST");
			result = editor.tasks().update().perform();
			editor.value(Department.NAME).set("SECOND");
			result.handle();
			assertEquals("FIRST", editor.value(Department.NAME).get());
			assertFalse(editor.entity().modified().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void persistResultLeavesMovedOnDetailEditorAlone() throws EntityValidationException {
		// the detail results of a master persist task replaced the details loaded for the master moved on to
		CONNECTION.startTransaction();
		try {
			insertExtra(10, "ten");
			insertExtra(40, "forty");
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(extraEditor)
							.foreignKey(DepartmentExtra.DEPARTMENT_FK)
							.present(extra -> extra.present(DepartmentExtra.DESCRIPTION))
							.build());
			departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(10)));
			extraEditor.value(DepartmentExtra.DESCRIPTION).set("ten edited");
			Result<Entity> result = departmentEditor.tasks().update().perform();
			departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(40)));
			result.handle();
			assertEquals(40, departmentEditor.value(Department.ID).get());
			assertEquals("forty", extraEditor.value(DepartmentExtra.DESCRIPTION).get());
			assertEquals("ten edited", CONNECTION.selectSingle(DepartmentExtra.DEPARTMENT.equalTo(10)).get(DepartmentExtra.DESCRIPTION));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void firstEditOfValueNotHeldByNewEntity() throws EntityValidationException {
		// the first value of one a new entity does not hold was taken for its initial value, not a change: unmodified,
		// its own original, not reverted, and a propagator gave its target null when the value returned to it
		CONNECTION.startTransaction();
		try {
			// a foreign key given no value, over a column whose default the database provides, both left out by defaults()
			TestEntityEditor editor = new TestEntityEditor(Assignment.TYPE, CONNECTION);
			EditorValue<Entity> departmentValue = editor.value(Assignment.DEPARTMENT_FK);
			EditorValue<Integer> priority = editor.value(Assignment.PRIORITY);
			departmentValue.propagate(Assignment.PRIORITY, department -> department == null ? null : department.get(Department.ID));
			editor.entity().defaults();
			assertFalse(editor.entity().get().contains(Assignment.DEPARTMENT_FK));
			assertFalse(editor.entity().get().contains(Assignment.PRIORITY));
			Entity accounting = CONNECTION.selectSingle(Department.ID.equalTo(10));
			Entity research = CONNECTION.selectSingle(Department.ID.equalTo(20));

			departmentValue.set(accounting);
			assertTrue(departmentValue.modified().is());
			assertNull(departmentValue.original());
			assertEquals(10, priority.get());
			assertTrue(priority.modified().is());
			assertNull(priority.original());
			assertTrue(editor.entity().modified().attributes().get().contains(Assignment.DEPARTMENT_FK));

			// back to the first value given is not back to the original, the value derived, not the target's original
			departmentValue.set(research);
			assertEquals(20, priority.get());
			departmentValue.set(accounting);
			assertEquals(10, priority.get());
			assertNull(departmentValue.original());

			// cleared, back where it started
			departmentValue.clear();
			assertFalse(departmentValue.modified().is());
			assertNull(priority.get());
			assertFalse(priority.modified().is());

			// a value reverts to null
			priority.set(1);
			assertTrue(priority.modified().is());
			priority.revert();
			assertNull(priority.get());
			assertFalse(priority.modified().is());

			// as it does with the entity, which then inserts as one never edited, the database providing the defaults
			departmentValue.set(research);
			editor.entity().revert();
			assertNull(departmentValue.get());
			assertNull(priority.get());
			Entity inserted = editor.insert();
			assertEquals(20, inserted.get(Assignment.DEPARTMENT));
			assertEquals(5, inserted.get(Assignment.PRIORITY));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}

		// a new entity built with a few values, set as the entity to edit
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		employeeEditor.entity().set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.NAME, "New")
						.build());
		EditorValue<Double> salary = employeeEditor.value(Employee.SALARY);
		assertFalse(employeeEditor.entity().get().contains(Employee.SALARY));
		salary.set(2500d);
		assertTrue(salary.modified().is());
		assertNull(salary.original());
		employeeEditor.entity().revert();
		assertNull(salary.get());

		// an existing entity is another matter, the first value of one it does not hold is the value loaded,
		// with which a later edit is compared, by the editor and by the optimistic lock
		employeeEditor.entity().set(CONNECTION.selectSingle(Select.where(Employee.ID.equalTo(3))
						.attributes(Employee.ID, Employee.NAME)
						.build()));
		assertFalse(employeeEditor.entity().get().contains(Employee.SALARY));
		salary.set(2975d);
		assertFalse(salary.modified().is());
		assertFalse(employeeEditor.entity().modified().is());
		salary.set(3000d);
		assertTrue(salary.modified().is());
		assertEquals(2975d, salary.original());
	}

	@Test
	void insertLeavesOutNullOfDatabaseDefault() throws EntityValidationException {
		// a value of a column whose default the database provides, given and then cleared, was inserted as null, where
		// one never given got the default, and the insert failed where the column is not nullable
		CONNECTION.startTransaction();
		try {
			TestEntityEditor editor = new TestEntityEditor(Assignment.TYPE, CONNECTION);
			editor.entity().defaults();
			Entity department = CONNECTION.selectSingle(Department.ID.equalTo(30));
			editor.value(Assignment.PRIORITY).set(1);
			editor.value(Assignment.PRIORITY).clear();
			editor.value(Assignment.STATUS).set(1);
			editor.value(Assignment.STATUS).clear();
			editor.value(Assignment.DEPARTMENT_FK).set(department);
			editor.value(Assignment.DEPARTMENT_FK).clear();
			// the editor holds the nulls
			assertTrue(editor.entity().get().contains(Assignment.PRIORITY));
			List<Entity> beforeInsert = new ArrayList<>();
			editor.events().before().insert().addConsumer(beforeInsert::addAll);

			Entity inserted = editor.insert();
			assertEquals(5, inserted.get(Assignment.PRIORITY));
			assertEquals(7, inserted.get(Assignment.STATUS));
			assertEquals(20, inserted.get(Assignment.DEPARTMENT));
			// the entity inserted is without them, as one in which they were never given
			assertFalse(beforeInsert.get(0).contains(Assignment.PRIORITY));
			assertFalse(beforeInsert.get(0).contains(Assignment.STATUS));
			assertFalse(beforeInsert.get(0).contains(Assignment.DEPARTMENT));

			// a value given is inserted
			editor.entity().defaults();
			editor.value(Assignment.PRIORITY).set(1);
			editor.value(Assignment.STATUS).set(2);
			inserted = editor.insert();
			assertEquals(1, inserted.get(Assignment.PRIORITY));
			assertEquals(2, inserted.get(Assignment.STATUS));

			// an entity given to insert is inserted as given, its null included
			inserted = editor.insert(singleton(CONNECTION.entities().entity(Assignment.TYPE)
							.with(Assignment.PRIORITY, null)
							.build())).iterator().next();
			assertNull(inserted.get(Assignment.PRIORITY));
			assertEquals(7, inserted.get(Assignment.STATUS));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void foreignKeyColumnDefaults() throws EntityValidationException {
		// a foreign key column whose default the database provides was overridden with null, as the foreign keys were
		// all set, null where they had no value, their columns along with them
		CONNECTION.startTransaction();
		try {
			TestEntityEditor editor = new TestEntityEditor(Assignment.TYPE, CONNECTION);
			editor.entity().defaults();
			Entity defaults = editor.entity().get();
			// the database provides it
			assertFalse(defaults.contains(Assignment.DEPARTMENT));
			// a default value of a foreign key column does not apply, a department not loaded, which nothing would show
			assertTrue(defaults.contains(Assignment.BACKUP_DEPARTMENT));
			assertNull(defaults.get(Assignment.BACKUP_DEPARTMENT));

			Entity inserted = editor.insert();
			assertEquals(20, inserted.get(Assignment.DEPARTMENT));
			assertNull(inserted.get(Assignment.BACKUP_DEPARTMENT));

			// a value given overrides both
			editor.entity().defaults();
			Entity department = CONNECTION.selectSingle(Department.ID.equalTo(30));
			editor.value(Assignment.DEPARTMENT_FK).set(department);
			editor.value(Assignment.BACKUP_DEPARTMENT_FK).set(department);
			inserted = editor.insert();
			assertEquals(30, inserted.get(Assignment.DEPARTMENT));
			assertEquals(30, inserted.get(Assignment.BACKUP_DEPARTMENT));

			// cleared, the column the database provides left out, the other null
			editor.entity().clear();
			assertFalse(editor.entity().get().contains(Assignment.DEPARTMENT));
			assertTrue(editor.entity().get().contains(Assignment.BACKUP_DEPARTMENT));
			assertNull(editor.entity().get().get(Assignment.BACKUP_DEPARTMENT));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void linkedComponentFollowsValueSetAgain() {
		// a component linked to an editor value kept what was typed when the edit came back changed, set again within
		// the edit by an edited() consumer, a propagator or a values().changed() consumer, the editor holding another value
		// an edited() consumer
		TestEntityEditor editor = new TestEntityEditor(Department.TYPE, CONNECTION);
		EditorValue<String> name = editor.value(Department.NAME);
		Value<String> component = linkedComponent(editor, name);
		name.edited().addConsumer(value -> {
			if (value != null) {
				name.set(value.toUpperCase());
			}
		});
		component.set("abc");
		assertEquals("ABC", name.get());
		assertEquals("ABC", component.get());

		// a propagator onto itself
		TestEntityEditor propagating = new TestEntityEditor(Department.TYPE, CONNECTION);
		EditorValue<String> propagatingName = propagating.value(Department.NAME);
		Value<String> propagatingComponent = linkedComponent(propagating, propagatingName);
		propagatingName.propagate(Department.NAME, value -> value == null ? null : value.toUpperCase());
		propagatingComponent.set("def");
		assertEquals("DEF", propagatingName.get());
		assertEquals("DEF", propagatingComponent.get());

		// a values().changed() consumer
		TestEntityEditor changed = new TestEntityEditor(Department.TYPE, CONNECTION);
		EditorValue<String> changedName = changed.value(Department.NAME);
		Value<String> changedComponent = linkedComponent(changed, changedName);
		changed.values().changed().addConsumer(attribute -> {
			String value = changedName.get();
			if (attribute.equals(Department.NAME) && value != null) {
				changedName.set(value.toUpperCase());
			}
		});
		changedComponent.set("ghi");
		assertEquals("GHI", changedName.get());
		assertEquals("GHI", changedComponent.get());

		// a pair deriving from each other, rounding, as net and gross would
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		EditorValue<Double> salary = employeeEditor.value(Employee.SALARY);
		Value<Double> salaryComponent = linkedComponent(employeeEditor, salary);
		salary.propagate(Employee.MANAGER_ID, value -> value == null ? null : (int) Math.round(value / 100));
		employeeEditor.value(Employee.MANAGER_ID).propagate(Employee.SALARY, value -> value == null ? null : value * 100d);
		salaryComponent.set(2549d);
		assertEquals(2500d, salary.get());
		assertEquals(2500d, salaryComponent.get());
	}

	private static <T> Value<T> linkedComponent(TestEntityEditor editor, EditorValue<T> editorValue) {
		editor.entity().defaults();
		Value<T> component = Value.nullable();
		component.link(editorValue);

		return component;
	}

	@Test
	void editNotifiesEveryValueChanged() {
		// an edit worked out the values depending on it one step only, so a value derived from a foreign key of an edited
		// column, a value derived from a column of an edited foreign key, and another foreign key sharing a column with an
		// edited one, all changed unnotified, a component showing them stale
		TestEntityEditor editor = new TestEntityEditor(SharedEmployee.TYPE, CONNECTION);
		List<Attribute<?>> notified = new ArrayList<>();
		editor.values().changed().addConsumer(notified::add);
		Value<Entity> viewComponent = Value.nullable();
		viewComponent.link(editor.value(SharedEmployee.DEPARTMENT_VIEW_FK));
		Value<String> nameComponent = Value.nullable();
		nameComponent.link(editor.value(SharedEmployee.DEPARTMENT_NAME));
		Value<String> codeComponent = Value.nullable();
		codeComponent.link(editor.value(SharedEmployee.DEPARTMENT_CODE));
		Entity jones = CONNECTION.selectSingle(SharedEmployee.ID.equalTo(3));
		assertEquals("RESEARCH", jones.get(SharedEmployee.DEPARTMENT_NAME));

		// the column edited, the name derived from its foreign key changing
		editor.entity().set(jones);
		notified.clear();
		editor.value(SharedEmployee.DEPARTMENT).set(30);
		assertNull(editor.value(SharedEmployee.DEPARTMENT_NAME).get());
		assertTrue(notified.contains(SharedEmployee.DEPARTMENT_NAME));
		assertNull(nameComponent.get());

		// the foreign key edited, the code derived from its column changing, and the other foreign key on the column
		editor.entity().set(jones);
		notified.clear();
		editor.value(SharedEmployee.DEPARTMENT_FK).set(CONNECTION.selectSingle(Department.ID.equalTo(10)));
		assertEquals("D10", editor.value(SharedEmployee.DEPARTMENT_CODE).get());
		assertNull(editor.value(SharedEmployee.DEPARTMENT_VIEW_FK).get());
		assertTrue(notified.containsAll(asList(SharedEmployee.DEPARTMENT_CODE, SharedEmployee.DEPARTMENT_VIEW_FK)));
		assertEquals("D10", codeComponent.get());
		assertNull(viewComponent.get());
		// each notified once, the edited value not among the depending ones
		assertEquals(new HashSet<>(notified).size(), notified.size());
	}

	@Test
	void propagatorsFollowValuesChangedAlongWithEdit() {
		// a propagator ran only for its own value being set, not for it changing along with another edit, as an edited()
		// consumer does, so a propagator on a foreign key's column never ran with the foreign key edited, as a combo box
		// edits it
		Entity jones = CONNECTION.selectSingle(SharedEmployee.ID.equalTo(3));
		Entity accounting = CONNECTION.selectSingle(Department.ID.equalTo(10));
		Entity research = CONNECTION.selectSingle(Department.ID.equalTo(20));

		// a foreign key's column, the foreign key edited
		TestEntityEditor columnEditor = new TestEntityEditor(SharedEmployee.TYPE, CONNECTION);
		columnEditor.value(SharedEmployee.DEPARTMENT).propagate(SharedEmployee.NAME, deptno -> "DEPTNO " + deptno);
		columnEditor.entity().set(jones);
		columnEditor.value(SharedEmployee.DEPARTMENT_FK).set(accounting);
		assertEquals("DEPTNO 10", columnEditor.value(SharedEmployee.NAME).get());
		// set in an entity, as the table model does, the editor's own left alone
		Entity copy = jones.copy().mutable();
		columnEditor.value(SharedEmployee.DEPARTMENT_FK).set(copy, accounting);
		assertEquals("DEPTNO 10", copy.get(SharedEmployee.NAME));

		// a derived value, its source edited
		TestEntityEditor derivedEditor = new TestEntityEditor(SharedEmployee.TYPE, CONNECTION);
		derivedEditor.value(SharedEmployee.DEPARTMENT_CODE).propagate(SharedEmployee.NAME, code -> "CODE " + code);
		derivedEditor.entity().set(jones);
		derivedEditor.value(SharedEmployee.DEPARTMENT).set(30);
		assertEquals("CODE D30", derivedEditor.value(SharedEmployee.NAME).get());

		// a foreign key, its column edited, its entity not loaded, so the deriver gets null
		TestEntityEditor foreignKeyEditor = new TestEntityEditor(SharedEmployee.TYPE, CONNECTION);
		foreignKeyEditor.value(SharedEmployee.DEPARTMENT_FK).propagate(SharedEmployee.NAME,
						department -> department == null ? "NONE" : department.get(Department.NAME));
		foreignKeyEditor.entity().set(jones);
		foreignKeyEditor.value(SharedEmployee.DEPARTMENT).set(30);
		assertEquals("NONE", foreignKeyEditor.value(SharedEmployee.NAME).get());

		// a second foreign key on a shared column, the first edited, the same
		TestEntityEditor sharedEditor = new TestEntityEditor(SharedEmployee.TYPE, CONNECTION);
		sharedEditor.value(SharedEmployee.DEPARTMENT_VIEW_FK).propagate(SharedEmployee.NAME,
						department -> department == null ? "NONE" : department.get(DepartmentView.NAME));
		sharedEditor.entity().set(jones);
		sharedEditor.value(SharedEmployee.DEPARTMENT_FK).set(accounting);
		assertEquals("NONE", sharedEditor.value(SharedEmployee.NAME).get());

		// a value changed along with an edit propagating back to the value edited, which goes no further
		TestEntityEditor cycleEditor = new TestEntityEditor(SharedEmployee.TYPE, CONNECTION);
		cycleEditor.value(SharedEmployee.DEPARTMENT).propagate(SharedEmployee.DEPARTMENT_FK, deptno -> research);
		cycleEditor.entity().set(jones);
		cycleEditor.value(SharedEmployee.DEPARTMENT_FK).set(accounting);
		assertEquals(research, cycleEditor.value(SharedEmployee.DEPARTMENT_FK).get());
		assertEquals(20, cycleEditor.value(SharedEmployee.DEPARTMENT).get());
	}

	@Test
	void validStatesFollowEntityValidation() {
		// a value's valid state and error were refreshed for the edited value only, stale when a rule spans attributes,
		// created from a validation of the value alone, disagreeing with the entity for an unmodified value of an existing
		// one, and validate() set the state but not the error
		AtomicBoolean required = new AtomicBoolean();
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.validator().set(new ManagerRequiredValidator(required));
		editor.entity().defaults();
		ObservableState managerValid = editor.value(Employee.MANAGER_ID).valid();
		Observable<String> managerError = editor.value(Employee.MANAGER_ID).error();
		assertTrue(managerValid.is());
		assertNull(managerError.get());

		// another value edited, requiring the manager
		editor.value(Employee.SALARY).set(6000d);
		assertFalse(editor.entity().valid().is());
		assertFalse(managerValid.is());
		assertEquals(ManagerRequiredValidator.MESSAGE, managerError.get());
		editor.value(Employee.SALARY).set(3000d);
		assertTrue(managerValid.is());
		assertNull(managerError.get());

		// a rule depending on something outside the entity, validated on request
		required.set(true);
		editor.value(Employee.MANAGER_ID).validate();
		assertFalse(managerValid.is());
		assertEquals(ManagerRequiredValidator.MESSAGE, managerError.get());
		required.set(false);
		editor.value(Employee.MANAGER_ID).validate();
		assertTrue(managerValid.is());

		// an existing row, the president, his null manager unmodified and so not validated, the entity valid
		Entity king = CONNECTION.selectSingle(Employee.ID.equalTo(8));
		editor.entity().set(king);
		assertTrue(editor.entity().valid().is());
		assertTrue(managerValid.is());
		assertNull(managerError.get());
		// as states created after the row was set
		TestEntityEditor after = new TestEntityEditor(Employee.TYPE, CONNECTION);
		after.validator().set(new ManagerRequiredValidator(required));
		after.entity().set(king);
		assertTrue(after.entity().valid().is());
		assertTrue(after.value(Employee.MANAGER_ID).valid().is());
		assertNull(after.value(Employee.MANAGER_ID).error().get());
	}

	@Test
	void twoValidationExceptionsForOneAttribute() {
		// a validator giving two validation exceptions for one attribute broke every edit, collecting them into a map
		// throwing on the duplicate
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.validator().set(new EntityValidator() {
			@Override
			public void validate(Entity entity) throws EntityValidationException {
				throw new EntityValidationException(asList(
								new AttributeValidationException(Employee.NAME, null, "first"),
								new AttributeValidationException(Employee.NAME, null, "second")));
			}
		});
		Observable<String> nameError = editor.value(Employee.NAME).error();
		editor.value(Employee.NAME).set("name");
		assertEquals("first", nameError.get());
		assertFalse(editor.entity().valid().is());
	}

	/**
	 * Requires a manager for a salary of 5000 or more, or while required is set
	 */
	private static final class ManagerRequiredValidator implements EntityValidator {

		private static final String MESSAGE = "A manager is required";

		private final AtomicBoolean required;

		private ManagerRequiredValidator(AtomicBoolean required) {
			this.required = required;
		}

		@Override
		public void validate(Entity entity, Attribute<?> attribute) throws AttributeValidationException {
			EntityValidator.super.validate(entity, attribute);
			Double salary = entity.get(Employee.SALARY);
			if (attribute.equals(Employee.MANAGER_ID) && entity.get(Employee.MANAGER_ID) == null
							&& (required.get() || salary != null && salary >= 5000)) {
				throw new AttributeValidationException(Employee.MANAGER_ID, null, MESSAGE);
			}
		}
	}

	@Test
	void resetNotifications() throws EntityValidationException {
		// a delete, and a detail editor a load found empty, notified replaced() with null, a delete never notifying
		// changed(), and an emptied detail editor notifying differently from a found one on the set path
		CONNECTION.startTransaction();
		try {
			insertExtra(10, "ten");
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(extraEditor)
							.foreignKey(DepartmentExtra.DEPARTMENT_FK)
							.present(extra -> extra.present(DepartmentExtra.DESCRIPTION))
							.build());
			List<Entity> masterChanged = new ArrayList<>();
			List<Entity> masterReplaced = new ArrayList<>();
			List<Entity> detailChanged = new ArrayList<>();
			List<Entity> detailReplaced = new ArrayList<>();
			AtomicInteger masterChanging = new AtomicInteger();
			departmentEditor.entity().addConsumer(masterChanged::add);
			departmentEditor.entity().replaced().addConsumer(masterReplaced::add);
			departmentEditor.entity().changing().addListener(masterChanging::incrementAndGet);
			extraEditor.entity().addConsumer(detailChanged::add);
			extraEditor.entity().replaced().addConsumer(detailReplaced::add);
			Entity ten = CONNECTION.selectSingle(Department.ID.equalTo(10));
			Entity forty = CONNECTION.selectSingle(Department.ID.equalTo(40));

			// the set path, a detail found and then none: changed, both
			departmentEditor.entity().set(ten);
			assertEquals("ten", detailChanged.get(0).get(DepartmentExtra.DESCRIPTION));
			detailChanged.clear();
			departmentEditor.entity().set(forty);
			assertEquals(1, detailChanged.size());
			assertNull(detailChanged.get(0).get(DepartmentExtra.DESCRIPTION));
			assertTrue(detailReplaced.isEmpty());

			// the replace path, a detail found and then none: replaced, both, never null
			departmentEditor.entity().replace(ten);
			assertEquals("ten", detailReplaced.get(0).get(DepartmentExtra.DESCRIPTION));
			detailReplaced.clear();
			departmentEditor.entity().replace(forty);
			assertEquals(1, detailReplaced.size());
			assertNotNull(detailReplaced.get(0));

			// a delete: changed with the defaults, not preceded by changing, the delete not being cancellable
			masterChanged.clear();
			masterReplaced.clear();
			masterChanging.set(0);
			departmentEditor.delete();
			assertEquals(1, masterChanged.size());
			assertFalse(masterChanged.get(0).exists());
			assertTrue(masterReplaced.isEmpty());
			assertEquals(0, masterChanging.get());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailValidatorJudgesEntityGiven() throws EntityValidationException {
		// a detail editor's validator read the detail editor's own present state, whatever the entity it was given, as a
		// table model's row, so another entity went unvalidated while the editor's detail was absent, and the other way
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(employee -> employee.present(Employee.NAME))
						.build());
		Entity present = CONNECTION.selectSingle(Employee.ID.equalTo(3)).copy().mutable();
		present.set(Employee.SALARY, 1d);// below the range
		Entity absent = present.copy().mutable();
		absent.set(Employee.NAME, null);

		// the detail editor's own detail absent
		employeeEditor.entity().defaults();
		assertFalse(employeeEditor.entity().present().is());
		assertThrows(EntityValidationException.class, () -> employeeEditor.validate(present));
		employeeEditor.validate(absent);

		// and present
		employeeEditor.value(Employee.NAME).set("Name");
		assertTrue(employeeEditor.entity().present().is());
		assertThrows(EntityValidationException.class, () -> employeeEditor.validate(present));
		employeeEditor.validate(absent);
	}

	@Test
	void detailEditorCycleRefused() {
		// a detail editor registered with itself, or with an editor it is a master of, was accepted, every reset and load
		// then overflowing the stack
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		assertThrows(IllegalArgumentException.class, () -> employeeEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.MANAGER_FK)
						.build()));
		employeeEditor.entity().defaults();

		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);
		departmentEditor.detail().add(EditorLink.builder()
						.editor(extraEditor)
						.foreignKey(DepartmentExtra.DEPARTMENT_FK)
						.build());
		assertThrows(IllegalArgumentException.class, () -> extraEditor.detail().add(EditorLink.builder()
						.editor(departmentEditor)
						.name("cycle")
						.entity((master, connection) -> null)
						.build()));
		departmentEditor.entity().defaults();
		departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(10)));

		// removed, the detail editor can be registered elsewhere, its former master included as its detail
		departmentEditor.detail().remove(DepartmentExtra.DEPARTMENT_FK);
		extraEditor.detail().add(EditorLink.builder()
						.editor(departmentEditor)
						.name("reversed")
						.entity((master, connection) -> null)
						.build());
		extraEditor.entity().defaults();
	}

	@Test
	void transientValueModifiesOnlyWhenConfigured() throws EntityValidationException {
		// a transient value made the entity modified by default, enabling an update which then threw, no modified values
		// found, and was listed among the modified attributes either way
		TestEntityEditor editor = new TestEntityEditor(SharedEmployee.TYPE, CONNECTION);
		editor.entity().set(CONNECTION.selectSingle(SharedEmployee.ID.equalTo(3)));
		editor.value(SharedEmployee.NOTES).set("notes");
		assertTrue(editor.value(SharedEmployee.NOTES).modified().is());
		assertFalse(editor.entity().modified().is());
		assertTrue(editor.entity().modified().attributes().get().isEmpty());
		assertThrows(IllegalStateException.class, editor.tasks()::update);
		// one configured to modify the entity, stored by a custom persistence
		editor.value(SharedEmployee.PASSWORD).set("secret");
		assertTrue(editor.entity().modified().is());
		assertEquals(singleton(SharedEmployee.PASSWORD), editor.entity().modified().attributes().get());
		editor.tasks().update();
	}

	@Test
	void detailEditorInsertWithEmptyDetail() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			departmentEditor.value(Department.ID).set(52);
			departmentEditor.value(Department.NAME).set("EmptyDetail");
			// No values set on detail -> not valid, not exists

			insert(departmentEditor);

			assertTrue(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());
			assertEquals(0, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(52))));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateDeletePath() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(employeeEditor.entity().exists().is());
			assertEquals(1, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(53))));

			// Clearing SALARY makes the detail not present -> update() deletes it (present=false, exists=true). A not-present
			// detail is valid (it will not be created); the delete path is driven by present/exists, not by validity.
			employeeEditor.value(Employee.SALARY).clear();
			assertTrue(employeeEditor.entity().valid().is());
			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);

			// Detail should be deleted
			assertFalse(employeeEditor.entity().exists().is());
			assertEquals(0, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(53))));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateInsertPath() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master only (detail invalid during insert)
			departmentEditor.value(Department.ID).set(54);
			departmentEditor.value(Department.NAME).set("InsPath");

			insert(departmentEditor);
			assertTrue(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());

			// Now set valid detail values
			employeeEditor.value(Employee.NAME).set("NewEmp");
			employeeEditor.value(Employee.SALARY).set(2500d);
			assertTrue(employeeEditor.entity().valid().is());
			assertTrue(departmentEditor.entity().modified().is());

			// Update master -> detail should be inserted
			update(departmentEditor);

			assertTrue(employeeEditor.entity().exists().is());
			assertEquals(1, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(54))));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateUpdatePath() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(employeeEditor.entity().exists().is());

			// Modify detail salary
			employeeEditor.value(Employee.SALARY).set(3000d);
			assertTrue(employeeEditor.entity().modified().is());
			assertTrue(employeeEditor.entity().valid().is());
			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);

			// Detail should be updated
			assertTrue(employeeEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().modified().is());
			assertEquals(3000d, employeeEditor.value(Employee.SALARY).get());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateNoOp() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(employeeEditor.entity().exists().is());

			// Modify only master, not detail
			departmentEditor.value(Department.NAME).set("NoOpMod");
			assertTrue(departmentEditor.entity().modified().is());
			assertFalse(employeeEditor.entity().modified().is());

			update(departmentEditor);

			// Detail unchanged
			assertTrue(employeeEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().modified().is());
			assertEquals(2000d, employeeEditor.value(Employee.SALARY).get());
			// Verify DB row unchanged
			assertEquals(1, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(56))));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorUpdateMasterUnmodifiedDetailModified() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(employeeEditor.entity().modified().is());
			// Master's modified() should be true via Updatable additional state
			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);

			// Detail should be updated
			assertTrue(employeeEditor.entity().exists().is());
			assertEquals(4000d, employeeEditor.value(Employee.SALARY).get());
			assertFalse(employeeEditor.entity().modified().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorDeleteWithExistingDetail() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(departmentEditor.entity().exists().is());
			assertTrue(employeeEditor.entity().exists().is());

			delete(departmentEditor);

			// Both should be deleted and editors cleared
			assertFalse(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());
			assertEquals(0, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(58))));
			assertEquals(0, CONNECTION.count(
							Count.where(Department.ID.equalTo(58))));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorDeleteWithNoDetail() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());

			// Insert master only (detail invalid)
			departmentEditor.value(Department.ID).set(59);
			departmentEditor.value(Department.NAME).set("NoDet");

			insert(departmentEditor);
			assertTrue(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());

			// Delete master -> should not fail even though no detail exists
			delete(departmentEditor);

			assertFalse(departmentEditor.entity().exists().is());
			assertEquals(0, CONNECTION.count(
							Count.where(Department.ID.equalTo(59))));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorFullLifecycle() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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
			assertTrue(departmentEditor.entity().exists().is());
			assertTrue(employeeEditor.entity().exists().is());

			// 2. Modify detail -> update master -> detail updated
			employeeEditor.value(Employee.SALARY).set(2000d);
			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);
			assertTrue(employeeEditor.entity().exists().is());
			assertEquals(2000d, employeeEditor.value(Employee.SALARY).get());

			// 3. Invalidate detail -> update master -> detail deleted
			employeeEditor.value(Employee.SALARY).clear();
			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);
			assertFalse(employeeEditor.entity().exists().is());
			assertEquals(0, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(60))));

			// 4. Set valid detail values -> update master -> detail re-inserted
			employeeEditor.value(Employee.NAME).set("EmpReborn");
			employeeEditor.value(Employee.SALARY).set(3000d);
			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);
			assertTrue(employeeEditor.entity().exists().is());
			assertEquals(1, CONNECTION.count(
							Count.where(Employee.DEPARTMENT.equalTo(60))));

			// 5. Delete master -> both gone
			delete(departmentEditor);
			assertFalse(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());
			assertEquals(0, CONNECTION.count(
							Count.where(Department.ID.equalTo(60))));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void multipleDetailEditors() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
			TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);
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
			assertTrue(departmentEditor.entity().exists().is());
			assertTrue(employeeEditor.entity().exists().is());
			assertTrue(extraEditor.entity().exists().is());

			// Modify one detail (employee salary), invalidate other (extra description is nullable
			// so to invalidate we use the employee; let's modify extra and invalidate employee instead)
			extraEditor.value(DepartmentExtra.DESCRIPTION).set("Updated info");
			employeeEditor.value(Employee.SALARY).clear(); // invalidate employee

			assertTrue(departmentEditor.entity().modified().is());

			update(departmentEditor);

			// Employee should be deleted (invalid), extra should be updated
			assertFalse(employeeEditor.entity().exists().is());
			assertTrue(extraEditor.entity().exists().is());
			assertEquals("Updated info", extraEditor.value(DepartmentExtra.DESCRIPTION).get());

			// Delete master -> all cleaned up
			delete(departmentEditor);
			assertFalse(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());
			assertFalse(extraEditor.entity().exists().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void multiSlotDetailEditorNoShuffleAfterUpdate() throws EntityValidationException {
		// Two FK detail editors share the same FK, distinguished by an offset/limit-based DetailSelect.
		// After an update that re-orders the rows, slots must retain their original primary keys —
		// PK-stable post-persist reload, not loader-based reshuffle.
		CONNECTION.startTransaction();
		try {
			EntityConnection connection = CONNECTION;
			Entity dept40 = connection.selectSingle(Department.ID.equalTo(40));
			connection.insertSelect(CONNECTION.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "ALPHA")
							.with(Employee.DEPARTMENT, 40)
							.with(Employee.SALARY, 2000d)
							.build());
			connection.insertSelect(CONNECTION.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "BETA")
							.with(Employee.DEPARTMENT, 40)
							.with(Employee.SALARY, 3000d)
							.build());

			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor first = new TestEntityEditor(Employee.TYPE, CONNECTION);
			TestEntityEditor second = new TestEntityEditor(Employee.TYPE, CONNECTION);

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
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void multiSlotDetailEditorDeleteRetainsOtherSlot() throws EntityValidationException {
		// Multi-slot setup; deleting one slot must not cause the loader to slide the remaining
		// row from the surviving slot's offset into a different slot.
		CONNECTION.startTransaction();
		try {
			EntityConnection connection = CONNECTION;
			Entity dept40 = connection.selectSingle(Department.ID.equalTo(40));
			connection.insertSelect(CONNECTION.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "ALPHA")
							.with(Employee.DEPARTMENT, 40)
							.with(Employee.SALARY, 2000d)
							.build());
			connection.insertSelect(CONNECTION.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "BETA")
							.with(Employee.DEPARTMENT, 40)
							.with(Employee.SALARY, 3000d)
							.build());

			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor first = new TestEntityEditor(Employee.TYPE, CONNECTION);
			TestEntityEditor second = new TestEntityEditor(Employee.TYPE, CONNECTION);

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
			assertTrue(first.entity().exists().is());
			assertTrue(second.entity().exists().is());

			// Trigger delete on slot "first" by clearing its salary -> present=false, exists=true.
			first.value(Employee.SALARY).clear();
			update(departmentEditor);

			assertFalse(first.entity().exists().is());
			// Slot "second" must still hold BETA — without PK-stable, the loader would shift
			// BETA into slot "first" (offset 0) and leave slot "second" empty.
			assertEquals(secondPk, second.entity().get().primaryKey());
			assertEquals("BETA", second.value(Employee.NAME).get());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorReplaceAfterMasterUpdatePreservesSlot() throws EntityValidationException {
		// FK detail with an additional load condition. Editing the row to no longer satisfy that
		// condition leaves it in the slot post-persist — the master's primary key is unchanged so
		// replace() doesn't trigger a reload, and the participating detail's own persist task
		// replaces its entity with the freshly-updated row. The next master selection re-runs the
		// loader and correctly empties the slot.
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor highEarnerEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);

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
			assertTrue(highEarnerEditor.entity().exists().is());

			// Edit salary to no longer satisfy the load condition.
			highEarnerEditor.value(Employee.SALARY).set(2500d);
			update(departmentEditor);

			// Same-PK replace doesn't reload; the slot retains the just-edited row even though
			// it no longer satisfies the load condition.
			assertEquals(highEarnerPk, highEarnerEditor.entity().get().primaryKey());
			assertEquals(2500d, highEarnerEditor.value(Employee.SALARY).get());

			// Master selection re-runs the loader; condition no longer matches -> slot empties.
			Entity dept50 = CONNECTION.selectSingle(Department.ID.equalTo(50));
			departmentEditor.entity().defaults();
			departmentEditor.entity().set(dept50);
			assertFalse(highEarnerEditor.entity().exists().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailEditorReplaceWithDifferentMasterReloadsDetail() {
		// replace() with a different master primary key reloads details via their loaders —
		// otherwise slots would hold the previous master's children. Same-PK replace remains a
		// silent value refresh that does not trigger a reload.
		CONNECTION.startTransaction();
		try {
			EntityConnection connection = CONNECTION;
			Entity deptA = connection.insertSelect(CONNECTION.entities().entity(Department.TYPE)
							.with(Department.ID, 71)
							.with(Department.NAME, "DEPT_A")
							.build());
			Entity empA = connection.insertSelect(CONNECTION.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "EMP_A")
							.with(Employee.DEPARTMENT, 71)
							.with(Employee.SALARY, 2000d)
							.build());
			Entity deptB = connection.insertSelect(CONNECTION.entities().entity(Department.TYPE)
							.with(Department.ID, 72)
							.with(Department.NAME, "DEPT_B")
							.build());
			Entity empB = connection.insertSelect(CONNECTION.entities().entity(Employee.TYPE)
							.with(Employee.NAME, "EMP_B")
							.with(Employee.DEPARTMENT, 72)
							.with(Employee.SALARY, 2000d)
							.build());

			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
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

			// clear() clears details (identity goes from real -> null).
			departmentEditor.entity().clear();
			assertFalse(employeeEditor.entity().exists().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailLoadFailedRefusesPersist() throws EntityValidationException {
		// a detail load which fails leaves the detail editors holding the previous master's details, so nothing
		// persists with the master until a load succeeds, where the previous master's details were persisted with it
		CONNECTION.startTransaction();
		try {
			insertExtra(40, "forty");
			insertExtra(10, "ten");
			Entity tenAgain = insertExtra(10, "ten again");// two rows where the link expects one at most
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(extraEditor)
							.foreignKey(DepartmentExtra.DEPARTMENT_FK)
							.present(extra -> extra.present(DepartmentExtra.DESCRIPTION))
							.build());
			departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(40)));
			Entity ten = CONNECTION.selectSingle(Department.ID.equalTo(10));
			assertThrows(IllegalStateException.class, () -> departmentEditor.entity().set(ten));
			// the master is 10, its detail editor cleared, where it held the detail of 40
			assertEquals(10, departmentEditor.value(Department.ID).get());
			assertFalse(extraEditor.entity().exists().is());
			assertNull(extraEditor.value(DepartmentExtra.DESCRIPTION).get());

			extraEditor.value(DepartmentExtra.DESCRIPTION).set("entered under 10");
			assertFalse(departmentEditor.entity().modified().is());
			assertTrue(departmentEditor.entity().modified().attributes().get().isEmpty());
			departmentEditor.value(Department.NAME).set("ACCOUNTS");
			assertTrue(departmentEditor.entity().modified().is());
			assertThrows(IllegalStateException.class, departmentEditor.tasks()::insert);
			assertThrows(IllegalStateException.class, departmentEditor.tasks()::update);
			assertThrows(IllegalStateException.class, departmentEditor.tasks()::delete);

			// a load which succeeds puts it right
			CONNECTION.delete(tenAgain.primaryKey());
			departmentEditor.entity().refresh();
			assertEquals("ten", extraEditor.value(DepartmentExtra.DESCRIPTION).get());
			extraEditor.value(DepartmentExtra.DESCRIPTION).set("edited under 10");
			assertTrue(departmentEditor.entity().modified().is());
			departmentEditor.update();
			assertEquals("edited under 10", CONNECTION.selectSingle(DepartmentExtra.DEPARTMENT.equalTo(10)).get(DepartmentExtra.DESCRIPTION));
			assertEquals("forty", CONNECTION.selectSingle(DepartmentExtra.DEPARTMENT.equalTo(40)).get(DepartmentExtra.DESCRIPTION));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailLoadPendingRefusesPersist() throws EntityValidationException {
		// until the detail load lands, asynchronous with a UI, the detail editors hold the previous master's details,
		// which were persisted with the new master, and counted as its modifications
		CONNECTION.startTransaction();
		try {
			insertExtra(10, "ten");
			HoldingEditor departmentEditor = new HoldingEditor(Department.TYPE, CONNECTION);
			TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(extraEditor)
							.foreignKey(DepartmentExtra.DEPARTMENT_FK)
							.present(extra -> extra.present(DepartmentExtra.DESCRIPTION))
							.build());
			departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(10)));
			extraEditor.value(DepartmentExtra.DESCRIPTION).set("discarded edit");
			assertTrue(departmentEditor.entity().modified().is());

			// moving on, discarding the edit, the detail load held in flight
			departmentEditor.hold();
			departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(40)));
			assertEquals(40, departmentEditor.value(Department.ID).get());
			assertEquals("discarded edit", extraEditor.value(DepartmentExtra.DESCRIPTION).get());
			assertFalse(departmentEditor.entity().modified().is());
			assertTrue(departmentEditor.entity().modified().attributes().get().isEmpty());
			assertThrows(IllegalStateException.class, departmentEditor.tasks()::insert);
			assertThrows(IllegalStateException.class, departmentEditor.tasks()::update);
			assertThrows(IllegalStateException.class, departmentEditor.tasks()::delete);

			departmentEditor.release();
			// department 40 has no extra
			assertFalse(extraEditor.entity().exists().is());
			assertNull(extraEditor.value(DepartmentExtra.DESCRIPTION).get());
			assertFalse(departmentEditor.entity().modified().is());
			departmentEditor.value(Department.NAME).set("OPS");
			departmentEditor.update();
			assertEquals("ten", CONNECTION.selectSingle(DepartmentExtra.DEPARTMENT.equalTo(10)).get(DepartmentExtra.DESCRIPTION));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailLoadPendingSurvivesReplace() {
		// replacing the entity with another instance of itself, as a table refresh does, while its detail load is in
		// flight, superseded the load, leaving the detail editors holding the previous master's details for good
		CONNECTION.startTransaction();
		try {
			insertExtra(10, "ten");
			HoldingEditor departmentEditor = new HoldingEditor(Department.TYPE, CONNECTION);
			TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(extraEditor)
							.foreignKey(DepartmentExtra.DEPARTMENT_FK)
							.present(extra -> extra.present(DepartmentExtra.DESCRIPTION))
							.build());
			departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(10)));

			departmentEditor.hold();
			departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(40)));
			departmentEditor.entity().replace(CONNECTION.selectSingle(Department.ID.equalTo(40)));
			assertEquals("ten", extraEditor.value(DepartmentExtra.DESCRIPTION).get());
			departmentEditor.release();
			assertFalse(extraEditor.entity().exists().is());
			assertNull(extraEditor.value(DepartmentExtra.DESCRIPTION).get());
			// no longer refused
			departmentEditor.tasks().delete();
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void detailStatesFollowMasterExistence() {
		// defaults() reset the detail editors before the master, computing what persists with it against the previous
		// master's existence, so the next master set read as modified until its details were loaded
		CONNECTION.startTransaction();
		try {
			insertExtra(10, "ten");
			insertExtra(20, "twenty");
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor extraEditor = new TestEntityEditor(DepartmentExtra.TYPE, CONNECTION);
			// the default present predicate, the detail always present
			departmentEditor.detail().add(EditorLink.builder()
							.editor(extraEditor)
							.foreignKey(DepartmentExtra.DEPARTMENT_FK)
							.build());
			departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(10)));
			departmentEditor.entity().defaults();
			assertFalse(departmentEditor.entity().modified().is());
			List<Boolean> modified = new ArrayList<>();
			departmentEditor.entity().modified().addConsumer(modified::add);
			departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(20)));
			assertTrue(modified.isEmpty());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	private static Entity insertExtra(int department, String description) {
		return CONNECTION.insertSelect(CONNECTION.entities().entity(DepartmentExtra.TYPE)
						.with(DepartmentExtra.DEPARTMENT, department)
						.with(DepartmentExtra.DESCRIPTION, description)
						.build());
	}

	@Test
	void detailEditorGatesMasterPersist() throws EntityValidationException {
		CONNECTION.startTransaction();
		try {
			TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
			TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
			departmentEditor.detail().add(EditorLink.builder()
							.editor(employeeEditor)
							.foreignKey(Employee.DEPARTMENT_FK)
							.present(EMPLOYEE_PRESENT)
							.build());
			departmentEditor.value(Department.ID).set(42);
			departmentEditor.value(Department.NAME).set("Test");
			employeeEditor.settings().insertEnabled().set(false);

			// A present detail that may not be inserted fails the master's insert, and the message says
			// which editor refused, the operation having been started on the master
			employeeEditor.value(Employee.NAME).set("Name");
			employeeEditor.value(Employee.SALARY).set(2_000d);
			assertTrue(employeeEditor.entity().present().is());
			assertTrue(assertThrows(IllegalStateException.class, () -> insert(departmentEditor))
							.getMessage().contains(Employee.TYPE.name()));

			// Absent, it is not persisted, so it gates nothing
			employeeEditor.value(Employee.NAME).set(null);
			assertFalse(employeeEditor.entity().present().is());
			insert(departmentEditor);
			assertTrue(departmentEditor.entity().exists().is());
			assertFalse(employeeEditor.entity().exists().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	private static void insert(TestEntityEditor editor) throws EntityValidationException {
		editor.tasks(editor.connection()).insert().perform().handle();
	}

	private static void update(TestEntityEditor editor) throws EntityValidationException {
		editor.tasks(editor.connection()).update().perform().handle();
	}

	private static void delete(TestEntityEditor editor) {
		editor.tasks(editor.connection()).delete().perform().handle();
	}

	@Test
	void columnComboBoxModelNotRetainedByPersistenceEvents() {
		WeakReference<FilterComboBoxModel<String>> reference = createColumnComboBoxModel();
		for (int i = 0; i < 20 && reference.get() != null; i++) {
			System.gc();
		}
		assertNull(reference.get());
	}

	private static WeakReference<FilterComboBoxModel<String>> createColumnComboBoxModel() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);

		return new WeakReference<>(editor.comboBoxModels().get(Employee.NAME));
	}

	@Test
	void propagateFunctionsOfSupertypes() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		EditorValue<Double> salary = editor.value(Employee.SALARY);
		Predicate<Object> present = Objects::nonNull;
		Function<Number, Integer> toInteger = value -> value == null ? null : value.intValue();
		salary.propagate(Employee.MANAGER_ID, present, toInteger);
		salary.set(10d);
		assertEquals(10, editor.value(Employee.MANAGER_ID).get());
	}

	@Test
	void propagateCycle() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		EditorValue<Double> salary = editor.value(Employee.SALARY);
		EditorValue<Integer> manager = editor.value(Employee.MANAGER_ID);
		//a cycle that converges, the second lap derives the value already there and stops on the equality check
		salary.propagate(Employee.MANAGER_ID, value -> value == null ? null : value.intValue());
		manager.propagate(Employee.SALARY, value -> value == null ? null : value.doubleValue());

		salary.set(10d);
		assertEquals(10, manager.get());

		manager.set(20);
		assertEquals(20d, salary.get());
	}

	@Test
	void propagateDivergentCycle() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		EditorValue<Double> salary = editor.value(Employee.SALARY);
		EditorValue<Integer> manager = editor.value(Employee.MANAGER_ID);
		//a cycle that does not converge, every lap moves both values, so the equality check propagateCycle()
		//rests on never fires and only the re-entrancy guard ends this
		salary.propagate(Employee.MANAGER_ID, value -> value == null ? null : value.intValue() + 1);
		manager.propagate(Employee.SALARY, value -> value == null ? null : value.doubleValue() + 1);

		//one lap: 10 derives a manager of 11, which derives a salary of 12, which goes no further
		salary.set(10d);
		assertEquals(11, manager.get());
		assertEquals(12d, salary.get());

		//and one lap from the other end
		manager.set(20);
		assertEquals(21d, salary.get());
		assertEquals(22, manager.get());
	}

	@Test
	void propagateDivergentSelf() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		EditorValue<String> name = editor.value(Employee.NAME);
		//the smallest cycle there is, and unlike propagateSelf() not idempotent
		name.propagate(Employee.NAME, value -> value == null ? null : value + "!");

		name.set("John");
		assertEquals("John!", name.get());
	}

	@Test
	void propagateSelf() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		EditorValue<String> name = editor.value(Employee.NAME);
		name.propagate(Employee.NAME, value -> value == null ? null : value.trim());

		name.set(" john ");
		assertEquals("john", name.get());
	}

	@Test
	void propagateEntityTransitive() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.NAME).propagate(Employee.SALARY, value -> (double) value.length());
		editor.value(Employee.SALARY).propagate(Employee.MANAGER_ID, Double::intValue);
		//a cycle back to the source, which is given its derived value and propagates no further
		editor.value(Employee.MANAGER_ID).propagate(Employee.NAME, String::valueOf);

		Entity employee = CONNECTION.entities().entity(Employee.TYPE).build();
		editor.value(Employee.NAME).set(employee, "Scott");

		assertEquals("5", employee.get(Employee.NAME));
		assertEquals(5d, employee.get(Employee.SALARY));
		assertEquals(5, employee.get(Employee.MANAGER_ID));
	}

	@Test
	void propagateConditional() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		//selecting a manager gives the department, clearing the manager leaves it
		editor.value(Employee.MANAGER_FK).propagate(Employee.DEPARTMENT_FK,
						Objects::nonNull, manager -> manager.get(Employee.DEPARTMENT_FK));
		Entity department = CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -4)
						.with(Department.NAME, "Accounting")
						.build();
		Entity manager = CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.DEPARTMENT_FK, department)
						.build();

		editor.value(Employee.MANAGER_FK).set(manager);
		assertEquals(department, editor.value(Employee.DEPARTMENT_FK).get());
		editor.value(Employee.MANAGER_FK).clear();
		assertEquals(department, editor.value(Employee.DEPARTMENT_FK).get());

		Entity employee = CONNECTION.entities().entity(Employee.TYPE).build();
		editor.value(Employee.MANAGER_FK).set(employee, manager);
		assertEquals(department, employee.get(Employee.DEPARTMENT_FK));
		editor.value(Employee.MANAGER_FK).set(employee, null);
		assertEquals(department, employee.get(Employee.DEPARTMENT_FK));
	}

	@Test
	void propagateConditionalOnNull() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.NAME).propagate(Employee.SALARY, Objects::isNull, name -> -1d);
		//a name to begin with, since clearing a new one returns it to its original, giving the salary its own
		editor.entity().set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "Scott")
						.build());

		editor.value(Employee.NAME).set("Bob");
		assertNull(editor.value(Employee.SALARY).get());
		editor.value(Employee.NAME).clear();
		assertEquals(-1d, editor.value(Employee.SALARY).get());
	}

	@Test
	void propagateConditionalNotTransitive() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> false, name -> 1d);
		editor.value(Employee.SALARY).propagate(Employee.MANAGER_ID, Double::intValue);

		editor.value(Employee.NAME).set("Scott");
		assertNull(editor.value(Employee.SALARY).get());
		assertNull(editor.value(Employee.MANAGER_ID).get());
	}

	@Test
	void propagateReplaces() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> 1d);
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> 2d);
		Entity employee = CONNECTION.entities().entity(Employee.TYPE).build();

		editor.value(Employee.NAME).set("Scott");
		editor.value(Employee.NAME).set(employee, "Scott");
		assertEquals(2d, editor.value(Employee.SALARY).get());
		assertEquals(2d, employee.get(Employee.SALARY));

		//a condition that doesn't hold leaves the target, the previous mapping being gone
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> false, name -> 3d);

		editor.value(Employee.NAME).set("Bob");
		editor.value(Employee.NAME).set(employee, "Bob");
		assertEquals(2d, editor.value(Employee.SALARY).get());
		assertEquals(2d, employee.get(Employee.SALARY));
	}

	@Test
	void propagateInTheOrderAdded() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		List<Attribute<?>> targets = asList(Employee.ID, Employee.DEPARTMENT, Employee.MANAGER_ID, Employee.SALARY);
		List<Attribute<?>> propagated = new ArrayList<>();
		targets.forEach(target -> propagateRecorded(editor, target, propagated));

		editor.value(Employee.NAME).set("Scott");
		assertEquals(targets, propagated);

		propagated.clear();
		editor.value(Employee.NAME).set(CONNECTION.entities().entity(Employee.TYPE).build(), "Scott");
		assertEquals(targets, propagated);
	}

	private static <T> void propagateRecorded(TestEntityEditor editor, Attribute<T> target, List<Attribute<?>> propagated) {
		editor.value(Employee.NAME).propagate(target, name -> {
			propagated.add(target);

			return false;
		}, name -> null);
	}

	@Test
	void defaultsPropagatePersisted() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		//foreign keys persist by default, the name and salary don't
		editor.value(Employee.DEPARTMENT_FK).propagate(Employee.NAME,
						department -> department == null ? null : department.get(Department.NAME));
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> name == null ? null : (double) name.length());
		Entity department = CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -8)
						.with(Department.NAME, "Accounting")
						.build();
		editor.value(Employee.DEPARTMENT_FK).set(department);
		editor.value(Employee.NAME).set("Typed by hand");
		List<String> names = new ArrayList<>();
		editor.value(Employee.NAME).edited().addConsumer(names::add);

		//the department kept, as if it had just been set
		editor.entity().defaults();
		assertEquals(department, editor.value(Employee.DEPARTMENT_FK).get());
		assertEquals("Accounting", editor.value(Employee.NAME).get());
		assertEquals(10d, editor.value(Employee.SALARY).get());
		//which is no edit
		assertTrue(names.isEmpty());
		assertFalse(editor.value(Employee.NAME).modified().is());
		assertFalse(editor.value(Employee.SALARY).modified().is());
	}

	@Test
	void defaultsPropagateNothingKept() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.DEPARTMENT_FK).propagate(Employee.NAME,
						department -> department == null ? null : department.get(Department.NAME));
		editor.value(Employee.MANAGER_FK).propagate(Employee.SALARY, manager -> false, manager -> 1d);
		editor.value(Employee.NAME).defaultValue().set(() -> "Default");
		editor.value(Employee.SALARY).defaultValue().set(() -> 5000d);

		//no department kept, so nothing is derived from it, the name keeps its default
		editor.entity().defaults();
		assertNull(editor.value(Employee.DEPARTMENT_FK).get());
		assertEquals("Default", editor.value(Employee.NAME).get());

		//a manager kept, but the condition doesn't hold
		editor.value(Employee.MANAGER_FK).set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 2)
						.build());
		editor.entity().defaults();
		assertNotNull(editor.value(Employee.MANAGER_FK).get());
		assertEquals(5000d, editor.value(Employee.SALARY).get());
	}

	@Test
	void defaultsPersistedTargetDerived() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		//both foreign keys persist
		editor.value(Employee.MANAGER_FK).propagate(Employee.DEPARTMENT_FK,
						Objects::nonNull, manager -> manager.get(Employee.DEPARTMENT_FK));
		Entity accounting = CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -9)
						.with(Department.NAME, "Accounting")
						.build();
		Entity research = CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -10)
						.with(Department.NAME, "Research")
						.build();
		Entity manager = CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 2)
						.with(Employee.DEPARTMENT_FK, research)
						.build();
		editor.value(Employee.MANAGER_FK).set(manager);
		editor.value(Employee.DEPARTMENT_FK).set(accounting);

		//the department derived from the manager kept, rather than the one kept
		editor.entity().defaults();
		assertEquals(manager, editor.value(Employee.MANAGER_FK).get());
		assertEquals(research, editor.value(Employee.DEPARTMENT_FK).get());
	}

	@Test
	void propagateNotEditable() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		//a read only value derived from the one edited, with one derived from it in turn
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> (double) name.length());
		editor.value(Employee.SALARY).propagate(Employee.MANAGER_ID, Double::intValue);
		editor.value(Employee.SALARY).editable().set(false);
		List<Double> salaries = new ArrayList<>();
		editor.value(Employee.SALARY).edited().addConsumer(salaries::add);

		editor.value(Employee.NAME).set("Scott");
		assertEquals(5d, editor.value(Employee.SALARY).get());
		assertEquals(5, editor.value(Employee.MANAGER_ID).get());
		assertEquals(singletonList(5d), salaries);

		//the value itself is no more editable for it
		assertThrows(IllegalStateException.class, () -> editor.value(Employee.SALARY).set(1d));
		assertEquals(5d, editor.value(Employee.SALARY).get());

		//nor does a validator stop applying, or a derived value it rejects leave the value editable
		editor.value(Employee.SALARY).addValidator(salary -> {
			if (salary != null && salary > 10) {
				throw new IllegalArgumentException();
			}
		});
		assertThrows(IllegalArgumentException.class, () -> editor.value(Employee.NAME).set("A longer name"));
		assertEquals(5d, editor.value(Employee.SALARY).get());
		assertThrows(IllegalStateException.class, () -> editor.value(Employee.SALARY).set(1d));
	}

	@Test
	void propagateEntityUnchanged() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> 1d);
		Entity employee = CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.NAME, "Scott")
						.with(Employee.SALARY, 1000d)
						.build();

		editor.value(Employee.NAME).set(employee, "Scott");
		assertEquals(1000d, employee.get(Employee.SALARY));

		editor.value(Employee.NAME).set(employee, "Bob");
		assertEquals(1d, employee.get(Employee.SALARY));
	}

	@Test
	void propagateEntityForeignKeyNotLoadedCleared() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.DEPARTMENT_FK).propagate(Employee.NAME, department -> "No department");
		//references a department, which isn't loaded
		Entity employee = CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.NAME, "Scott")
						.with(Employee.DEPARTMENT, 10)
						.build();

		editor.value(Employee.DEPARTMENT_FK).set(employee, null);
		assertNull(employee.get(Employee.DEPARTMENT));
		assertEquals("No department", employee.get(Employee.NAME));
	}

	@Test
	void propagateTheSameInEditorAndEntity() {
		//on itself
		assertPropagatesTheSame(editor -> editor.value(Employee.NAME).propagate(Employee.NAME, name -> name + "!"));
		//a pair deriving from each other, converging
		assertPropagatesTheSame(editor -> {
			editor.value(Employee.NAME).propagate(Employee.SALARY, name -> (double) name.length());
			editor.value(Employee.SALARY).propagate(Employee.MANAGER_ID, Double::intValue);
			editor.value(Employee.MANAGER_ID).propagate(Employee.SALARY, Integer::doubleValue);
		});
		//and not converging
		assertPropagatesTheSame(editor -> {
			editor.value(Employee.NAME).propagate(Employee.MANAGER_ID, String::length);
			editor.value(Employee.MANAGER_ID).propagate(Employee.NAME, manager -> "Manager " + manager);
		});
		//a cycle through a third
		assertPropagatesTheSame(editor -> {
			editor.value(Employee.NAME).propagate(Employee.SALARY, name -> (double) name.length());
			editor.value(Employee.SALARY).propagate(Employee.MANAGER_ID, Double::intValue);
			editor.value(Employee.MANAGER_ID).propagate(Employee.NAME, String::valueOf);
		});
		//a target reached by two routes
		assertPropagatesTheSame(editor -> {
			editor.value(Employee.NAME).propagate(Employee.MANAGER_ID, name -> 1);
			editor.value(Employee.NAME).propagate(Employee.SALARY, name -> 2d);
			editor.value(Employee.MANAGER_ID).propagate(Employee.SALARY, manager -> 3d);
		});
	}

	private static void assertPropagatesTheSame(Consumer<TestEntityEditor> propagators) {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		propagators.accept(editor);
		Entity employee = CONNECTION.entities().entity(Employee.TYPE).build();

		editor.value(Employee.NAME).set("Scott");
		editor.value(Employee.NAME).set(employee, "Scott");

		assertEquals(editor.value(Employee.NAME).get(), employee.get(Employee.NAME));
		assertEquals(editor.value(Employee.SALARY).get(), employee.get(Employee.SALARY));
		assertEquals(editor.value(Employee.MANAGER_ID).get(), employee.get(Employee.MANAGER_ID));
	}

	@Test
	void revertDoesNotPropagate() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		//the name is defined before the manager it is derived from, and before the salary derived from it,
		//so a derived value is reverted both before and after its source
		editor.value(Employee.MANAGER_ID).propagate(Employee.NAME, manager -> manager == null ? null : "Manager " + manager);
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> name == null ? null : (double) name.length());
		editor.entity().set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "Scott")
						.with(Employee.MANAGER_ID, 99)
						.with(Employee.SALARY, 1000d)
						.build());

		editor.value(Employee.MANAGER_ID).set(7);
		assertEquals("Manager 7", editor.value(Employee.NAME).get());
		assertEquals(9d, editor.value(Employee.SALARY).get());

		editor.entity().revert();
		assertEquals(99, editor.value(Employee.MANAGER_ID).get());
		assertEquals("Scott", editor.value(Employee.NAME).get());
		assertEquals(1000d, editor.value(Employee.SALARY).get());
		assertFalse(editor.entity().get().modified());
	}

	@Test
	void revertValueRestoresTarget() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> name == null ? null : (double) name.length());
		editor.entity().set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "Scott")
						.with(Employee.SALARY, 1000d)
						.build());

		editor.value(Employee.NAME).set("Bob");
		assertEquals(3d, editor.value(Employee.SALARY).get());

		//reverting a single value is an edit of that value, back to its original, so the target is back to its own
		editor.value(Employee.NAME).revert();
		assertEquals("Scott", editor.value(Employee.NAME).get());
		assertEquals(1000d, editor.value(Employee.SALARY).get());
		assertFalse(editor.entity().get().modified());
	}

	@Test
	void propagateBackToOriginal() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		//a chain, which unwinds
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> name == null ? null : (double) name.length());
		editor.value(Employee.SALARY).propagate(Employee.MANAGER_ID, salary -> salary == null ? null : salary.intValue());
		Entity employee = CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "Scott")
						.with(Employee.SALARY, 1000d)
						.with(Employee.MANAGER_ID, 99)
						.build();
		editor.entity().set(employee);

		editor.value(Employee.NAME).set("Bob");
		assertEquals(3d, editor.value(Employee.SALARY).get());
		assertEquals(3, editor.value(Employee.MANAGER_ID).get());

		//set back by hand, not reverted
		editor.value(Employee.NAME).set("Scott");
		assertEquals(1000d, editor.value(Employee.SALARY).get());
		assertEquals(99, editor.value(Employee.MANAGER_ID).get());
		assertFalse(editor.entity().get().modified());

		Entity copy = employee.copy().mutable();
		editor.value(Employee.NAME).set(copy, "Bob");
		assertEquals(3d, copy.get(Employee.SALARY));
		editor.value(Employee.NAME).set(copy, "Scott");
		assertEquals(1000d, copy.get(Employee.SALARY));
		assertEquals(99, copy.get(Employee.MANAGER_ID));
		assertFalse(copy.modified());
	}

	@Test
	void propagateFirstValueDerives() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.NAME).propagate(Employee.SALARY, name -> name == null ? null : (double) name.length());
		//a row without a name, which then records no original, and is not modified, when given one
		editor.entity().set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.SALARY, 1000d)
						.build());

		editor.value(Employee.NAME).set("Bob");
		assertEquals(3d, editor.value(Employee.SALARY).get());

		Entity employee = CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.SALARY, 1000d)
						.build();
		editor.value(Employee.NAME).set(employee, "Bob");
		assertEquals(3d, employee.get(Employee.SALARY));
	}

	@Test
	void propagateConditionalBackToOriginal() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.MANAGER_FK).propagate(Employee.DEPARTMENT_FK,
						Objects::nonNull, manager -> manager.get(Employee.DEPARTMENT_FK));
		Entity accounting = CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -5)
						.with(Department.NAME, "Accounting")
						.build();
		Entity research = CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -6)
						.with(Department.NAME, "Research")
						.build();
		Entity manager = CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 2)
						.with(Employee.DEPARTMENT_FK, research)
						.build();
		//no manager to begin with
		editor.entity().set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "Scott")
						.with(Employee.DEPARTMENT_FK, accounting)
						.with(Employee.MANAGER_FK, null)
						.build());

		editor.value(Employee.MANAGER_FK).set(manager);
		assertEquals(research, editor.value(Employee.DEPARTMENT_FK).get());
		//back to no manager, which the condition doesn't accept, so the department stays
		editor.value(Employee.MANAGER_FK).clear();
		assertEquals(research, editor.value(Employee.DEPARTMENT_FK).get());
	}

	@Test
	void revertNotEditable() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.entity().set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "Scott")
						.build());
		editor.value(Employee.NAME).set("Bob");
		editor.value(Employee.NAME).editable().set(false);

		assertThrows(IllegalStateException.class, () -> editor.value(Employee.NAME).revert());
		assertEquals("Bob", editor.value(Employee.NAME).get());

		editor.entity().revert();
		assertEquals("Scott", editor.value(Employee.NAME).get());
		assertFalse(editor.entity().get().modified());
	}

	@Test
	void foreignKeyUpdatedElsewhere() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		Entity department = CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -1)
						.with(Department.NAME, "Accounting")
						.build();
		editor.value(Employee.DEPARTMENT_FK).set(department);

		List<Entity> foreignKeyValues = new ArrayList<>();
		List<Integer> columnValues = new ArrayList<>();
		List<Object> edited = new ArrayList<>();
		editor.value(Employee.DEPARTMENT_FK).addConsumer(foreignKeyValues::add);
		editor.value(Employee.DEPARTMENT).addConsumer(columnValues::add);
		editor.value(Employee.DEPARTMENT_FK).edited().addConsumer(edited::add);
		editor.value(Employee.DEPARTMENT).edited().addConsumer(edited::add);

		Entity updated = department.copy().mutable();
		updated.set(Department.NAME, "Bookkeeping");
		updated.save();

		persistenceEvents(Department.TYPE).updated().accept(singletonMap(department, updated));

		//the referenced entity is equal to the previous one, being the same row, but must still replace it
		assertEquals("Bookkeeping", editor.value(Employee.DEPARTMENT_FK).get().get(Department.NAME));
		//without ever travelling through null, which the reference column would have followed
		assertEquals(1, foreignKeyValues.size());
		assertEquals("Bookkeeping", foreignKeyValues.get(0).get(Department.NAME));
		assertTrue(columnValues.isEmpty());
		//not an edit
		assertTrue(edited.isEmpty());
	}

	@Test
	void foreignKeyReplacedOrRevertedNotEdited() {
		// edited() fired for a foreign key replaced because the referenced entity was updated elsewhere, and for
		// EditorEntity.revert(), so a consumer modified a record nobody had edited, or left a reverted one modified
		CONNECTION.startTransaction();
		try {
			TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
			// derives the name from the department, as the unit price of an invoice line from its track
			editor.value(Employee.DEPARTMENT_FK).edited().addConsumer(department ->
							editor.value(Employee.NAME).set(department == null ? null : department.get(Department.NAME)));
			Entity jones = CONNECTION.selectSingle(Employee.ID.equalTo(3));
			editor.entity().set(jones);
			Entity department = jones.get(Employee.DEPARTMENT_FK);
			Entity renamed = department.copy().mutable();
			renamed.set(Department.NAME, "RENAMED");
			renamed.save();
			persistenceEvents(Department.TYPE).updated().accept(singletonMap(department, renamed));
			assertEquals("RENAMED", editor.value(Employee.DEPARTMENT_FK).get().get(Department.NAME));
			assertEquals("JONES", editor.value(Employee.NAME).get());
			assertFalse(editor.entity().modified().is());

			// sets the manager from the salary, an attribute before it, which the entity restores first
			TestEntityEditor reverting = new TestEntityEditor(Employee.TYPE, CONNECTION);
			reverting.value(Employee.SALARY).edited().addConsumer(salary -> reverting.value(Employee.MANAGER_ID).set(5));
			reverting.entity().set(jones);
			reverting.value(Employee.SALARY).set(3000d);
			assertEquals(5, reverting.value(Employee.MANAGER_ID).get());
			reverting.entity().revert();
			assertEquals(jones.get(Employee.MANAGER_ID), reverting.value(Employee.MANAGER_ID).get());
			assertFalse(reverting.entity().modified().is());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void foreignKeyReplacedDoesNotPropagate() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		editor.value(Employee.DEPARTMENT_FK).propagate(Employee.NAME,
						department -> department == null ? null : department.get(Department.NAME));
		Entity department = CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -3)
						.with(Department.NAME, "Accounting")
						.build();
		editor.entity().set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "Typed by hand")
						.with(Employee.DEPARTMENT_FK, department)
						.build());

		Entity updated = department.copy().mutable();
		updated.set(Department.NAME, "Bookkeeping");
		updated.save();

		persistenceEvents(Department.TYPE).updated().accept(singletonMap(department, updated));
		assertEquals("Bookkeeping", editor.value(Employee.DEPARTMENT_FK).get().get(Department.NAME));
		//a row updated elsewhere is no edit of this one
		assertEquals("Typed by hand", editor.value(Employee.NAME).get());
		assertFalse(editor.entity().get().modified());

		persistenceEvents(Department.TYPE).deleted().accept(singleton(updated));
		assertNull(editor.value(Employee.DEPARTMENT_FK).get());
		assertEquals("Typed by hand", editor.value(Employee.NAME).get());

		//an edit still propagates
		editor.value(Employee.DEPARTMENT_FK).set(CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -7)
						.with(Department.NAME, "Research")
						.build());
		assertEquals("Research", editor.value(Employee.NAME).get());

		//and the original department gives the original name back
		editor.value(Employee.DEPARTMENT_FK).set(updated);
		assertEquals("Typed by hand", editor.value(Employee.NAME).get());
	}

	@Test
	void foreignKeyNotLoadedCleared() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		//references a department, which isn't loaded
		editor.entity().set(CONNECTION.entities().entity(Employee.TYPE)
						.with(Employee.ID, 1)
						.with(Employee.NAME, "Scott")
						.with(Employee.DEPARTMENT, 10)
						.build());
		assertNull(editor.value(Employee.DEPARTMENT_FK).get());
		editor.value(Employee.DEPARTMENT_FK).propagate(Employee.NAME, department -> "No department");
		List<Integer> columnValues = new ArrayList<>();
		editor.value(Employee.DEPARTMENT).edited().addConsumer(columnValues::add);
		assertFalse(editor.value(Employee.DEPARTMENT).modified().is());

		//its value null both before and after, it is the reference which changes
		editor.value(Employee.DEPARTMENT_FK).clear();
		assertNull(editor.value(Employee.DEPARTMENT).get());
		assertTrue(editor.value(Employee.DEPARTMENT).modified().is());
		assertEquals(1, columnValues.size());
		assertEquals("No department", editor.value(Employee.NAME).get());

		//now it references nothing, so nothing changes
		editor.value(Employee.NAME).set("Scott");
		editor.value(Employee.DEPARTMENT_FK).clear();
		assertEquals(1, columnValues.size());
		assertEquals("Scott", editor.value(Employee.NAME).get());
	}

	@Test
	void foreignKeyPersistenceIgnoresEditable() {
		TestEntityEditor editor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		Entity department = CONNECTION.entities().entity(Department.TYPE)
						.with(Department.ID, -2)
						.with(Department.NAME, "Research")
						.build();
		editor.value(Employee.DEPARTMENT_FK).set(department);
		editor.value(Employee.DEPARTMENT_FK).editable().set(false);

		Entity updated = department.copy().mutable();
		updated.set(Department.NAME, "Science");
		updated.save();

		persistenceEvents(Department.TYPE).updated().accept(singletonMap(department, updated));
		assertEquals("Science", editor.value(Employee.DEPARTMENT_FK).get().get(Department.NAME));

		persistenceEvents(Department.TYPE).deleted().accept(singleton(updated));
		assertNull(editor.value(Employee.DEPARTMENT_FK).get());
	}

	@Test
	void editorLinkBuildDoesNotClaimPresent() {
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build();

		//the link was never added, the predicate is unclaimed
		Predicate<Entity> present = employee -> employee.present(Employee.SALARY);
		employeeEditor.entity().present().predicate().set(present);
		assertSame(present, employeeEditor.entity().present().predicate().get());
	}

	@Test
	void detailEditorRemovalUnlinks() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);

		// The configuration the editor carries before registration, all three of which registration replaces
		Predicate<Entity> present = employee -> employee.present(Employee.NAME);
		employeeEditor.entity().present().predicate().set(present);
		EntityValidator validator = new SalaryValidator();
		employeeEditor.validator().set(validator);
		assertTrue(employeeEditor.value(Employee.DEPARTMENT_FK).persist().is());

		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build());

		assertSame(EMPLOYEE_PRESENT, employeeEditor.entity().present().predicate().get());
		assertInstanceOf(DetailForeignKeyValidator.class, employeeEditor.validator().get());
		assertFalse(employeeEditor.value(Employee.DEPARTMENT_FK).persist().is());
		assertFalse(departmentEditor.entity().modified().additional().get().isEmpty());
		// The master foreign key is framework-managed while registered, its persistence is locked false
		assertThrows(IllegalArgumentException.class, () -> employeeEditor.value(Employee.DEPARTMENT_FK).persist().set(true));
		assertDoesNotThrow(() -> employeeEditor.value(Employee.DEPARTMENT_FK).persist().set(false));

		departmentEditor.detail().remove(Employee.DEPARTMENT_FK);

		assertThrows(IllegalArgumentException.class, () -> departmentEditor.detail().get(Employee.DEPARTMENT_FK));
		// Restored, not merely dropped — the pre-registration configuration is back
		assertSame(present, employeeEditor.entity().present().predicate().get());
		assertSame(validator, employeeEditor.validator().get());
		assertTrue(employeeEditor.value(Employee.DEPARTMENT_FK).persist().is());
		assertTrue(departmentEditor.entity().modified().additional().get().isEmpty());

		// and unlocked, all three locks released
		Predicate<Entity> replacement = employee -> employee.present(Employee.SALARY);
		employeeEditor.entity().present().predicate().set(replacement);
		assertSame(replacement, employeeEditor.entity().present().predicate().get());
		EntityValidator replacementValidator = new EntityValidator() {};
		employeeEditor.validator().set(replacementValidator);
		assertSame(replacementValidator, employeeEditor.validator().get());
		employeeEditor.value(Employee.DEPARTMENT_FK).persist().set(true);
		assertTrue(employeeEditor.value(Employee.DEPARTMENT_FK).persist().is());

		// The listeners are detached, the master no longer hears the detail
		employeeEditor.value(Employee.NAME).set("Name");
		assertFalse(departmentEditor.entity().modified().attributes().get().contains(Employee.NAME));

		// and the editor can be registered again
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build());
		assertSame(EMPLOYEE_PRESENT, employeeEditor.entity().present().predicate().get());
		assertInstanceOf(DetailForeignKeyValidator.class, employeeEditor.validator().get());
		assertFalse(employeeEditor.value(Employee.DEPARTMENT_FK).persist().is());
	}

	@Test
	void detailEditorWarnings() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		employeeEditor.validator().set(new SalaryValidator());
		EditorValue<Double> salary = employeeEditor.value(Employee.SALARY);
		employeeEditor.value(Employee.NAME).set("Name");
		salary.set(4_000d);
		assertTrue(salary.warned().is());
		assertEquals("Unusually high", salary.warning().get());

		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build());

		// Registration wraps the validator, which used to drop its warnings on the floor, the wrapper
		// implementing EntityValidator without delegating warning()
		salary.set(4_100d);
		assertTrue(employeeEditor.entity().present().is());
		assertTrue(salary.warned().is());
		assertEquals("Unusually high", salary.warning().get());

		// Absent, the detail row is not going to exist, so there is nothing to warn about. Cleared in
		// the same edit that made it absent, warnings being recomputed after present()
		employeeEditor.value(Employee.NAME).set(null);
		assertFalse(employeeEditor.entity().present().is());
		assertFalse(salary.warned().is());
		assertNull(salary.warning().get());

		// and back, in the edit that makes it present again
		employeeEditor.value(Employee.NAME).set("Name");
		assertTrue(employeeEditor.entity().present().is());
		assertTrue(salary.warned().is());
		assertEquals("Unusually high", salary.warning().get());

		// Removal hands the validator back, warnings with it
		departmentEditor.detail().remove(Employee.DEPARTMENT_FK);
		salary.set(4_200d);
		assertTrue(salary.warned().is());
		assertEquals("Unusually high", salary.warning().get());
	}

	@Test
	void failedRegistrationLeavesNothingBehind() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		Predicate<Entity> present = employee -> employee.present(Employee.NAME);
		employeeEditor.entity().present().predicate().set(present);
		EntityValidator validator = new SalaryValidator();
		employeeEditor.validator().set(validator);
		State persist = employeeEditor.value(Employee.DEPARTMENT_FK).persist();
		assertTrue(persist.is());
		//an application validator on a framework-managed value makes the registration fail halfway,
		//after the present predicate has been claimed and the validator wrapped
		Value.Validator<Boolean> rejectFalse = value -> {
			if (Boolean.FALSE.equals(value)) {
				throw new IllegalArgumentException("no false");
			}
		};
		persist.addValidator(rejectFalse);

		assertThrows(IllegalArgumentException.class, () -> departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build()));

		//the failed registration left nothing behind - not registered, nothing claimed, nothing locked
		assertThrows(IllegalArgumentException.class, () -> departmentEditor.detail().get(Employee.DEPARTMENT_FK));
		assertSame(present, employeeEditor.entity().present().predicate().get());
		assertSame(validator, employeeEditor.validator().get());
		assertTrue(persist.is());
		assertTrue(departmentEditor.entity().modified().additional().get().isEmpty());

		//and the editor is registrable once the obstacle is gone
		persist.removeValidator(rejectFalse);
		assertDoesNotThrow(() -> departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build()));
	}

	@Test
	void editorLinkRegistersOnce() {
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor otherDepartmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(Employee.TYPE, CONNECTION);
		EditorLink link = EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build();
		departmentEditor.detail().add(link);

		// Registering the same link with the same master collides on the link key, the more specific error
		assertTrue(assertThrows(IllegalArgumentException.class, () -> departmentEditor.detail().add(link))
						.getMessage().contains("already exists"));

		// Registering it with a second master of the same type would otherwise silently share one detail
		// editor between two masters, each claiming its present predicate and validator
		assertTrue(assertThrows(IllegalArgumentException.class, () -> otherDepartmentEditor.detail().add(link))
						.getMessage().contains("already registered"));
		// The rejected registration left nothing behind
		assertTrue(otherDepartmentEditor.entity().modified().additional().get().isEmpty());
		assertSame(EMPLOYEE_PRESENT, employeeEditor.entity().present().predicate().get());

		// A distinct link wrapping the same detail editor is rejected too — the present lock alone
		// would not catch it, it compares predicate instances and these two share one
		EditorLink second = EditorLink.builder()
						.editor(employeeEditor)
						.foreignKey(Employee.DEPARTMENT_FK)
						.present(EMPLOYEE_PRESENT)
						.build();
		assertTrue(assertThrows(IllegalArgumentException.class, () -> otherDepartmentEditor.detail().add(second))
						.getMessage().contains("already registered"));

		// Released by removal, the link is registrable again
		departmentEditor.detail().remove(Employee.DEPARTMENT_FK);
		assertDoesNotThrow(() -> otherDepartmentEditor.detail().add(link));
	}

	@Test
	void adjustedValue() throws Exception {
		// the entity adjusting the value it is set to, trimming a string here, the editor follows the value it ends up with
		CONNECTION.startTransaction();
		try {
			try (Statement statement = ((LocalEntityConnection) CONNECTION).connection().createStatement()) {
				statement.executeUpdate("update employees.department set dname = 'SALES ' where deptno = 30");
			}
			TestEntityEditor editor = new TestEntityEditor(Department.TYPE, CONNECTION);
			editor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(30)));
			List<String> edited = new ArrayList<>();
			editor.value(Department.NAME).edited().addConsumer(edited::add);
			// the value requested equal to the one loaded, the trimmed one it ends up with not
			editor.value(Department.NAME).set("SALES ");
			assertEquals("SALES", editor.value(Department.NAME).get());
			assertTrue(editor.entity().modified().is());
			assertEquals(singletonList("SALES"), edited);
			// the value requested not equal to the current one, the trimmed one equal, nothing changing
			editor.value(Department.NAME).set("SALES  ");
			assertEquals(singletonList("SALES"), edited);
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void persistenceFactory() {
		// DetailPersistenceFactory, registered for DetailDomain, provides the persistence of departments, asked per editor
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		assertInstanceOf(DepartmentPersistence.class, departmentEditor.persistence().get());
		assertNotSame(departmentEditor.persistence().get(), new TestEntityEditor(Department.TYPE, CONNECTION).persistence().get());
		// and the default for employees
		assertSame(EntityPersistence.Factory.DEFAULT, new TestEntityEditor(Employee.TYPE, CONNECTION).persistence().get());

		EntityPersistence persistence = new EntityPersistence() {};
		departmentEditor.persistence().set(persistence);
		assertSame(persistence, departmentEditor.persistence().get());
		// null restores the factory's
		departmentEditor.persistence().set(null);
		assertInstanceOf(DepartmentPersistence.class, departmentEditor.persistence().get());
	}

	@Test
	void populate() throws EntityValidationException {
		// a value an existing entity is set without, a column not selected, could not be edited and saved,
		// the editor having nothing to compare the edit with
		CONNECTION.startTransaction();
		try {
			Entity jones = CONNECTION.selectSingle(LazyEmployee.ID.equalTo(3));
			assertFalse(jones.contains(LazyEmployee.DATA));
			byte[] jonesData = CONNECTION.select(LazyEmployee.DATA, LazyEmployee.ID.equalTo(3)).get(0);

			// not populated unless asked for
			TestEntityEditor editor = new TestEntityEditor(LazyEmployee.TYPE, CONNECTION);
			editor.entity().set(jones);
			assertFalse(editor.entity().get().contains(LazyEmployee.DATA));

			editor = new TestEntityEditor(LazyEmployee.TYPE, CONNECTION);
			EditorValue<byte[]> data = editor.value(LazyEmployee.DATA);
			data.populate().set(true);
			List<Entity> changed = new ArrayList<>();
			editor.entity().addConsumer(changed::add);
			editor.entity().set(jones);
			// loaded, as the value the entity holds, not a change
			assertArrayEquals(jonesData, data.get());
			assertFalse(data.modified().is());
			assertFalse(editor.entity().modified().is());
			// the entity notified is the one set, with the value loaded
			assertEquals(1, changed.size());
			assertArrayEquals(jonesData, changed.get(0).get(LazyEmployee.DATA));

			// so an edit is one, and is saved
			data.set(new byte[] {1, 2});
			assertTrue(data.modified().is());
			assertTrue(editor.entity().modified().is());
			editor.update();
			assertArrayEquals(new byte[] {1, 2}, CONNECTION.select(LazyEmployee.DATA, LazyEmployee.ID.equalTo(3)).get(0));
			assertArrayEquals(new byte[] {1, 2}, data.get());
			assertFalse(editor.entity().modified().is());

			// refreshed, the value held being selected
			editor.entity().refresh();
			assertArrayEquals(new byte[] {1, 2}, data.get());

			// an entity holding the value is left as it is
			editor.entity().set(CONNECTION.selectSingle(Select.where(LazyEmployee.ID.equalTo(7))
							.include(LazyEmployee.DATA)
							.build()));
			assertNotNull(data.get());
			assertFalse(editor.entity().modified().is());

			// the key of no row, nothing to load and nothing failing
			editor.entity().set(CONNECTION.entities().entity(LazyEmployee.TYPE)
							.with(LazyEmployee.ID, -42)
							.with(LazyEmployee.NAME, "nobody")
							.build());
			assertFalse(editor.entity().get().contains(LazyEmployee.DATA));
			assertEquals(4, changed.size());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void populateNewEntity() throws EntityValidationException {
		// a new entity holds a value populated, as null, so the entity inserted does as well
		CONNECTION.startTransaction();
		try {
			TestEntityEditor editor = new TestEntityEditor(LazyEmployee.TYPE, CONNECTION);
			editor.entity().defaults();
			assertFalse(editor.entity().get().contains(LazyEmployee.DATA));

			EditorValue<byte[]> data = editor.value(LazyEmployee.DATA);
			data.populate().set(true);
			editor.entity().defaults();
			assertTrue(editor.entity().get().contains(LazyEmployee.DATA));
			assertNull(data.get());
			editor.entity().clear();
			assertTrue(editor.entity().get().contains(LazyEmployee.DATA));

			editor.value(LazyEmployee.NAME).set("New");
			editor.value(LazyEmployee.SALARY).set(2500d);
			editor.value(LazyEmployee.DEPARTMENT_FK).set(CONNECTION.selectSingle(Department.ID.equalTo(10)));
			Entity inserted = editor.insert();
			assertTrue(inserted.contains(LazyEmployee.DATA));
			assertTrue(editor.entity().get().contains(LazyEmployee.DATA));

			// and can be edited
			data.set(new byte[] {5});
			assertTrue(editor.entity().modified().is());
			editor.update();
			assertArrayEquals(new byte[] {5}, CONNECTION.select(LazyEmployee.DATA, LazyEmployee.ID.equalTo(inserted.get(LazyEmployee.ID))).get(0));
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void populateForeignKey() {
		// a foreign key populated covers its columns, the entity referenced loaded along with them
		Entity partial = CONNECTION.selectSingle(Select.where(LazyEmployee.ID.equalTo(3))
						.attributes(LazyEmployee.ID, LazyEmployee.NAME)
						.build());
		assertFalse(partial.contains(LazyEmployee.DEPARTMENT));

		TestEntityEditor editor = new TestEntityEditor(LazyEmployee.TYPE, CONNECTION);
		editor.value(LazyEmployee.DEPARTMENT_FK).populate().set(true);
		editor.entity().set(partial);
		assertEquals(20, editor.value(LazyEmployee.DEPARTMENT).get());
		assertEquals("RESEARCH", editor.value(LazyEmployee.DEPARTMENT_FK).get().get(Department.NAME));
		assertFalse(editor.entity().modified().is());
		// only what was asked for
		assertFalse(editor.entity().get().contains(LazyEmployee.SALARY));
		assertFalse(editor.entity().get().contains(LazyEmployee.DATA));

		// an attribute which is neither a column nor a foreign key has nothing to load
		assertThrows(IllegalArgumentException.class, () -> editor.value(LazyEmployee.NOTES).populate().set(true));
	}

	@Test
	void populateFresherInstance() throws EntityValidationException {
		// a fresher instance of the row, a table refresh, does not bring the value, which is loaded anew: kept as it
		// was loaded, it was as old as its load, an update refused as modified by someone else once it had changed
		CONNECTION.startTransaction();
		try {
			Entity jones = CONNECTION.selectSingle(LazyEmployee.ID.equalTo(3));
			byte[] jonesData = CONNECTION.select(LazyEmployee.DATA, LazyEmployee.ID.equalTo(3)).get(0);
			TestEntityEditor editor = new TestEntityEditor(LazyEmployee.TYPE, CONNECTION);
			EditorValue<byte[]> data = editor.value(LazyEmployee.DATA);
			data.populate().set(true);
			editor.entity().set(jones);
			List<byte[]> dataNotified = new ArrayList<>();
			data.addConsumer(dataNotified::add);

			Entity fresher = jones.copy().mutable();
			fresher.set(LazyEmployee.NAME, "FRESHER");
			fresher.save();
			List<Entity> replaced = new ArrayList<>();
			editor.entity().replaced().addConsumer(replaced::add);
			editor.entity().replace(fresher);
			assertEquals("FRESHER", editor.value(LazyEmployee.NAME).get());
			assertArrayEquals(jonesData, data.get());
			assertFalse(editor.entity().modified().is());
			// the value is held throughout, unchanged it is not notified
			assertTrue(dataNotified.isEmpty());
			// the entity notified holds it as well
			assertEquals(1, replaced.size());
			assertArrayEquals(jonesData, replaced.get(0).get(LazyEmployee.DATA));

			// an edit of it is discarded, as every edit is by a replace
			data.set(new byte[] {9});
			editor.entity().replace(fresher);
			assertArrayEquals(jonesData, data.get());
			assertFalse(editor.entity().modified().is());

			// changed elsewhere, the fresher instance brings it up to date
			Entity elsewhere = CONNECTION.selectSingle(Select.where(LazyEmployee.ID.equalTo(3))
							.include(LazyEmployee.DATA)
							.build()).copy().mutable();
			elsewhere.set(LazyEmployee.DATA, new byte[] {7});
			CONNECTION.update(elsewhere);
			dataNotified.clear();
			editor.entity().replace(CONNECTION.selectSingle(LazyEmployee.ID.equalTo(3)));
			assertArrayEquals(new byte[] {7}, data.get());
			assertEquals(1, dataNotified.size());
			assertFalse(editor.entity().modified().is());
			// so the entity can be updated
			editor.value(LazyEmployee.NAME).set("EDITED");
			editor.update();
			assertEquals("EDITED", CONNECTION.selectSingle(LazyEmployee.ID.equalTo(3)).get(LazyEmployee.NAME));

			// another row is loaded for
			editor.entity().replace(CONNECTION.selectSingle(LazyEmployee.ID.equalTo(7)));
			assertArrayEquals(CONNECTION.select(LazyEmployee.DATA, LazyEmployee.ID.equalTo(7)).get(0), data.get());
		}
		finally {
			CONNECTION.rollbackTransaction();
		}
	}

	@Test
	void populateWhileLoading() {
		// asynchronous with a UI, the value arrives after the entity is set
		Entity jones = CONNECTION.selectSingle(LazyEmployee.ID.equalTo(3));
		byte[] jonesData = CONNECTION.select(LazyEmployee.DATA, LazyEmployee.ID.equalTo(3)).get(0);
		HoldingEditor editor = new HoldingEditor(LazyEmployee.TYPE, CONNECTION);
		EditorValue<byte[]> data = editor.value(LazyEmployee.DATA);
		data.populate().set(true);
		List<Entity> changed = new ArrayList<>();
		editor.entity().addConsumer(changed::add);
		List<byte[]> edited = new ArrayList<>();
		data.edited().addConsumer(edited::add);

		editor.hold();
		editor.entity().set(jones);
		assertFalse(editor.entity().get().contains(LazyEmployee.DATA));
		assertTrue(changed.isEmpty());
		// edits meanwhile, of a value held and of the one being loaded
		editor.value(LazyEmployee.NAME).set("EDITED");
		data.set(new byte[] {9});
		// and a fresher instance of the row, which leaves the load to land
		editor.entity().replace(CONNECTION.selectSingle(LazyEmployee.ID.equalTo(3)));
		editor.value(LazyEmployee.NAME).set("EDITED");

		editor.release();
		// the one loaded is overwritten, not an edit, the other survives
		assertArrayEquals(jonesData, data.get());
		assertFalse(data.modified().is());
		assertEquals(1, edited.size());
		assertEquals("EDITED", editor.value(LazyEmployee.NAME).get());
		assertEquals(1, changed.size());
		assertArrayEquals(jonesData, changed.get(0).get(LazyEmployee.DATA));
	}

	@Test
	void populateDetailEditor() {
		// a detail editor's values are populated when its detail is loaded
		TestEntityEditor departmentEditor = new TestEntityEditor(Department.TYPE, CONNECTION);
		TestEntityEditor employeeEditor = new TestEntityEditor(LazyEmployee.TYPE, CONNECTION);
		employeeEditor.value(LazyEmployee.DATA).populate().set(true);
		departmentEditor.detail().add(EditorLink.builder()
						.editor(employeeEditor)
						.name("first")
						.entity((master, connection) -> connection.select(Select.where(LazyEmployee.DEPARTMENT_FK.equalTo(master))
										.orderBy(ascending(LazyEmployee.ID))
										.limit(1)
										.build()).stream().findFirst().orElse(null))
						.build());
		departmentEditor.entity().set(CONNECTION.selectSingle(Department.ID.equalTo(20)));
		assertEquals(3, employeeEditor.value(LazyEmployee.ID).get());
		assertNotNull(employeeEditor.value(LazyEmployee.DATA).get());
		assertFalse(employeeEditor.entity().modified().is());
		assertFalse(departmentEditor.entity().modified().is());
	}

	private static class TestEntityEditor extends AbstractEntityEditor<TestEntityEditor> {

		private TestEntityEditor(EntityType entityType, EntityConnection connection) {
			this(entityType, connection, new TestComponentModels());
		}

		private TestEntityEditor(EntityType entityType, EntityConnection connection, TestComponentModels componentModels) {
			super(entityType, connection, componentModels);
		}

		@Override
		public TestEntityEditor create(EntityType entityType) {
			return new TestEntityEditor(entityType, connection());
		}

		@Override
		public TestEntityEditor create(EntityType entityType, ComponentModels componentModels) {
			return new TestEntityEditor(entityType, connection(), (TestComponentModels) componentModels);
		}
	}

	/**
	 * Holds a detail load back, as the background thread does, the window in which the master holds the new entity and
	 * its detail editors the previous master's details, and drops it when superseded, as its result would be.
	 */
	private static final class HoldingEditor extends TestEntityEditor {

		private boolean hold;
		private EditorTask<Entity> held;

		private HoldingEditor(EntityType entityType, EntityConnection connection) {
			super(entityType, connection);
		}

		private void hold() {
			hold = true;
		}

		private void release() {
			hold = false;
			EditorTask<Entity> task = held;
			held = null;
			if (task != null) {
				super.execute(task);
			}
		}

		@Override
		protected void execute(EditorTask<Entity> task) {
			if (hold) {
				held = task;
			}
			else {
				super.execute(task);
			}
		}

		@Override
		protected void supersede() {
			held = null;
			super.supersede();
		}
	}

	private static final class TestComponentModels implements EntityEditor.ComponentModels {}
}
