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

import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.attribute.Attribute;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;

import java.util.UUID;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.attribute.Column.Generator.*;

public final class DetailDomain extends DomainModel {

	public static final DomainType DOMAIN = domainType(DetailDomain.class);

	public DetailDomain() {
		super(DOMAIN);
		add(department(), employee(), departmentExtra(), assignment(), departmentView(), sharedEmployee(), lazyEmployee(),
						departmentNote());
	}

	public interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		Column<Integer> ID = TYPE.integerColumn("deptno");
		Column<String> NAME = TYPE.stringColumn("dname");
	}

	EntityDefinition department() {
		return Department.TYPE.as()
						.attributes(
										Department.ID.as()
														.primaryKey()
														.updatable(true)
														.nullable(false),
										Department.NAME.as()
														.column()
														.maximumLength(14)
														.nullable(false))
						.build();
	}

	public interface Employee {
		EntityType TYPE = DOMAIN.entityType("employees.employee");

		Column<Integer> ID = TYPE.integerColumn("empno");
		Column<String> NAME = TYPE.stringColumn("ename");
		Column<Double> SALARY = TYPE.doubleColumn("sal");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		Column<Integer> MANAGER_ID = TYPE.integerColumn("mgr");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
		ForeignKey MANAGER_FK = TYPE.foreignKey("manager_fk", MANAGER_ID, Employee.ID);
	}

	public interface DepartmentExtra {
		EntityType TYPE = DOMAIN.entityType("employees.department_extra");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		Column<String> DESCRIPTION = TYPE.stringColumn("description");
		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_extra_dept_fk", DEPARTMENT, Department.ID);
	}

	/**
	 * Foreign key columns with defaults, one provided by the database, the other by the domain,
	 * and two columns whose defaults the database provides, one of them not nullable
	 */
	public interface Assignment {
		EntityType TYPE = DOMAIN.entityType("employees.assignment");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		Column<Integer> BACKUP_DEPARTMENT = TYPE.integerColumn("backup_deptno");
		Column<Integer> PRIORITY = TYPE.integerColumn("priority");
		Column<Integer> STATUS = TYPE.integerColumn("status");
		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("assignment_dept_fk", DEPARTMENT, Department.ID);
		ForeignKey BACKUP_DEPARTMENT_FK = TYPE.foreignKey("assignment_backup_dept_fk", BACKUP_DEPARTMENT, Department.ID);
	}

	EntityDefinition assignment() {
		return Assignment.TYPE.as()
						.attributes(
										Assignment.ID.as()
														.primaryKey()
														.generator(identity()),
										Assignment.DEPARTMENT.as()
														.column()
														.withDefault(true),
										Assignment.DEPARTMENT_FK.as()
														.foreignKey(),
										Assignment.BACKUP_DEPARTMENT.as()
														.column()
														.defaultValue(10),
										Assignment.BACKUP_DEPARTMENT_FK.as()
														.foreignKey(),
										Assignment.PRIORITY.as()
														.column()
														.withDefault(true),
										Assignment.STATUS.as()
														.column()
														.nullable(false)
														.withDefault(true))
						.build();
	}

	/**
	 * The department table, read only, for a second foreign key on the department column of {@link SharedEmployee}
	 */
	public interface DepartmentView {
		EntityType TYPE = DOMAIN.entityType("employees.department_view");

		Column<Integer> ID = TYPE.integerColumn("deptno");
		Column<String> NAME = TYPE.stringColumn("dname");
	}

	EntityDefinition departmentView() {
		return DepartmentView.TYPE.as()
						.attributes(
										DepartmentView.ID.as()
														.primaryKey(),
										DepartmentView.NAME.as()
														.column())
						.table("employees.department")
						.readOnly(true)
						.build();
	}

	/**
	 * The employee table, with two foreign keys sharing the department column, the second read only on it, and a value
	 * derived from the first foreign key and one from the column
	 */
	public interface SharedEmployee {
		EntityType TYPE = DOMAIN.entityType("employees.shared_employee");

		Column<Integer> ID = TYPE.integerColumn("empno");
		Column<String> NAME = TYPE.stringColumn("ename");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
		ForeignKey DEPARTMENT_VIEW_FK = TYPE.foreignKey("dept_view_fk", DEPARTMENT, DepartmentView.ID);
		Attribute<String> DEPARTMENT_NAME = TYPE.stringAttribute("dept_name");
		Attribute<String> DEPARTMENT_CODE = TYPE.stringAttribute("dept_code");
		Attribute<String> NOTES = TYPE.stringAttribute("notes");
		Attribute<String> PASSWORD = TYPE.stringAttribute("password");
	}

	EntityDefinition sharedEmployee() {
		return SharedEmployee.TYPE.as()
						.attributes(
										SharedEmployee.ID.as()
														.primaryKey(),
										SharedEmployee.NAME.as()
														.column(),
										SharedEmployee.DEPARTMENT.as()
														.column(),
										SharedEmployee.DEPARTMENT_FK.as()
														.foreignKey(),
										SharedEmployee.DEPARTMENT_VIEW_FK.as()
														.foreignKey()
														.readOnly(SharedEmployee.DEPARTMENT),
										SharedEmployee.DEPARTMENT_NAME.as()
														.derived()
														.from(SharedEmployee.DEPARTMENT_FK)
														.with(values -> {
															Entity department = values.get(SharedEmployee.DEPARTMENT_FK);

															return department == null ? null : department.get(Department.NAME);
														}),
										SharedEmployee.DEPARTMENT_CODE.as()
														.derived()
														.from(SharedEmployee.DEPARTMENT)
														.with(values -> values.get(SharedEmployee.DEPARTMENT) == null ? null : "D" + values.get(SharedEmployee.DEPARTMENT)),
										SharedEmployee.NOTES.as()
														.attribute(),
										// stored by a custom persistence, by other means
										SharedEmployee.PASSWORD.as()
														.attribute()
														.modifies(true))
						.table("employees.employee")
						.build();
	}

	/**
	 * A detail of a department keyed by a generator preserving a key given, with generated
	 * columns which are not the key, one made by the column's default, unique
	 */
	public interface DepartmentNote {
		EntityType TYPE = DOMAIN.entityType("employees.department_note");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		Column<String> NOTE = TYPE.stringColumn("note");
		Column<Integer> SEQ_NO = TYPE.integerColumn("seq_no");
		Column<UUID> UUID = TYPE.column("uuid", UUID.class);
		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("department_note_dept_fk", DEPARTMENT, Department.ID);
	}

	EntityDefinition departmentNote() {
		return DepartmentNote.TYPE.as()
						.attributes(
										DepartmentNote.ID.as()
														.primaryKey()
														.generator(preserving(identity())),
										DepartmentNote.DEPARTMENT.as()
														.column()
														.nullable(false),
										DepartmentNote.DEPARTMENT_FK.as()
														.foreignKey(),
										DepartmentNote.NOTE.as()
														.column(),
										DepartmentNote.SEQ_NO.as()
														.column()
														.generator(preserving(sequence("employees.department_note_seq"))),
										// made by the database, a value given kept, as the sync's UUID
										DepartmentNote.UUID.as()
														.column()
														.generator(preserving(columnDefault())))
						.build();
	}

	EntityDefinition departmentExtra() {
		return DepartmentExtra.TYPE.as()
						.attributes(
										DepartmentExtra.ID.as()
														.primaryKey()
														.generator(identity()),
										DepartmentExtra.DEPARTMENT.as()
														.column()
														.nullable(false),
										DepartmentExtra.DEPARTMENT_FK.as()
														.foreignKey(),
										DepartmentExtra.DESCRIPTION.as()
														.column())
						.build();
	}

	EntityDefinition employee() {
		return Employee.TYPE.as()
						.attributes(
										Employee.ID.as()
														.primaryKey()
														.generator(sequence("employees.employee_seq")),
										Employee.NAME.as()
														.column()
														.nullable(false),
										Employee.DEPARTMENT.as()
														.column()
														.nullable(false),
										Employee.DEPARTMENT_FK.as()
														.foreignKey(),
										Employee.MANAGER_ID.as()
														.column(),
										Employee.MANAGER_FK.as()
														.foreignKey(),
										Employee.SALARY.as()
														.column()
														.nullable(false)
														.range(2000d, 10_000d))
						.build();
	}

	/**
	 * The employee table, with its data column not selected by default, and its manager, an employee
	 */
	public interface LazyEmployee {
		EntityType TYPE = DOMAIN.entityType("employees.lazy_employee");

		Column<Integer> ID = TYPE.integerColumn("empno");
		Column<String> NAME = TYPE.stringColumn("ename");
		Column<Double> SALARY = TYPE.doubleColumn("sal");
		Column<Integer> DEPARTMENT = TYPE.integerColumn("deptno");
		Column<Integer> MANAGER_ID = TYPE.integerColumn("mgr");
		Column<byte[]> DATA = TYPE.byteArrayColumn("data");
		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("dept_fk", DEPARTMENT, Department.ID);
		ForeignKey MANAGER_FK = TYPE.foreignKey("manager_fk", MANAGER_ID, LazyEmployee.ID);
		Attribute<String> NOTES = TYPE.stringAttribute("notes");
	}

	EntityDefinition lazyEmployee() {
		return LazyEmployee.TYPE.as()
						.attributes(
										LazyEmployee.ID.as()
														.primaryKey()
														.generator(sequence("employees.employee_seq")),
										LazyEmployee.NAME.as()
														.column(),
										LazyEmployee.SALARY.as()
														.column(),
										LazyEmployee.DEPARTMENT.as()
														.column(),
										LazyEmployee.DEPARTMENT_FK.as()
														.foreignKey(),
										LazyEmployee.MANAGER_ID.as()
														.column(),
										LazyEmployee.MANAGER_FK.as()
														.foreignKey(),
										LazyEmployee.DATA.as()
														.column()
														.selected(false),
										LazyEmployee.NOTES.as()
														.attribute())
						.table("employees.employee")
						.build();
	}
}
