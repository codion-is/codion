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
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.demos.employees.minimal;

import is.codion.common.rmi.client.Clients;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.DomainModel;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.KeyGenerator;
import is.codion.framework.domain.entity.attribute.Column;
import is.codion.framework.domain.entity.attribute.ForeignKey;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.model.component.EntityComboBoxModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import java.time.LocalDate;
import java.util.List;
import java.util.Locale;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

/**
 * Employees minimal application demo
 */
public final class EmployeesMinimalApp {

	/*
	 * The domain model identifier.
	 */
	static final DomainType DOMAIN = domainType(EmployeesMinimalApp.class);

	/*
	 * We start by defining attributes for the columns in the EMPLOYEES.DEPARTMENT table.
	 */
	interface Department {
		EntityType TYPE = DOMAIN.entityType("employees.department");

		Column<Integer> DEPARTMENT_NO = TYPE.integerColumn("department_no");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<String> LOCATION = TYPE.stringColumn("location");
	}

	/*
	 * And for the columns in the EMPLOYEES.EMPLOYEE table.
	 */
	interface Employee {
		EntityType TYPE = DOMAIN.entityType("employees.employee");

		Column<Integer> ID = TYPE.integerColumn("id");
		Column<String> NAME = TYPE.stringColumn("name");
		Column<Integer> DEPARTMENT_NO = TYPE.integerColumn("department_no");
		Column<String> JOB = TYPE.stringColumn("job");
		Column<Double> SALARY = TYPE.doubleColumn("salary");
		Column<Double> COMMISSION = TYPE.doubleColumn("commission");
		Column<Integer> MANAGER_ID = TYPE.integerColumn("manager_id");
		Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");

		ForeignKey DEPARTMENT_FK = TYPE.foreignKey("department_fk", DEPARTMENT_NO, Department.DEPARTMENT_NO);
		ForeignKey MANAGER_FK = TYPE.foreignKey("manager_fk", MANAGER_ID, Employee.ID);
	}

	/**
	 * This class initializes the domain model based on the EMPLOYEES schema
	 */
	private static final class Employees extends DomainModel {

		public Employees() {
			super(DOMAIN);
			/*
			 * We then define the entity based on the EMPLOYEES.DEPARTMENT table
			 */
			add(Department.TYPE.define(
											Department.DEPARTMENT_NO.define()
															.primaryKey(),
											Department.DEPARTMENT_NO.define()
															.column()
															.caption("Department name")
															.searchable(true)
															.nullable(false)
															.maximumLength(14),
											Department.LOCATION.define()
															.column()
															.caption("Department location")
															.maximumLength(13))
							.caption("Departments")
							.stringFactory(Department.NAME)
							.build());
			/*
			 * We then define the entity based on the EMPLOYEES.EMPLOYEE table,
			 * note the foreign keys, referencing the
			 * department as well as the manager
			 */
			add(Employee.TYPE.define(
											Employee.ID.define()
															.primaryKey(),
											Employee.NAME.define()
															.column()
															.caption("Name")
															.searchable(true)
															.nullable(false)
															.maximumLength(10),
											Employee.DEPARTMENT_NO.define()
															.column()
															.nullable(false),
											Employee.DEPARTMENT_FK.define()
															.foreignKey()
															.caption("Department"),
											Employee.JOB.define()
															.column()
															.caption("Job")
															.nullable(false)
															.maximumLength(9),
											Employee.SALARY.define()
															.column()
															.caption("Salary")
															.nullable(false)
															.maximumFractionDigits(2)
															.valueRange(1000, 10000),
											Employee.COMMISSION.define()
															.column()
															.caption("Commission")
															.maximumFractionDigits(2),
											Employee.MANAGER_ID.define()
															.column(),
											Employee.MANAGER_FK.define()
															.foreignKey()
															.caption("Manager"),
											Employee.HIREDATE.define()
															.column()
															.caption("Hiredate")
															.nullable(false))
							.keyGenerator(KeyGenerator.sequence("employees.emp_seq"))
							.caption("Employees")
							.stringFactory(Employee.NAME)
							.build());
		}
	}

	/**
	 * We extend the default entity edit model to provide a custom
	 * combo box model for the manager property
	 */
	public static final class EmployeeEditModel extends SwingEntityEditModel {

		public EmployeeEditModel(EntityConnectionProvider connectionProvider) {
			super(Employee.TYPE, connectionProvider);
			//initialize the combo box models now, otherwise it
			//happens on the EDT later when the combo boxes are created
			initializeComboBoxModels(Employee.MANAGER_FK, Employee.DEPARTMENT_FK);
		}

		/**
		 * We override this method to add a query condition to the manager combo box model
		 * so that is only shows managers.
		 */
		@Override
		public EntityComboBoxModel createComboBoxModel(ForeignKey foreignKey) {
			if (foreignKey.equals(Employee.MANAGER_FK)) {
				return EntityComboBoxModel.builder(Employee.TYPE, connectionProvider())
								.condition(() -> Employee.JOB.in("Manager", "President"))
								.build();
			}

			return super.createComboBoxModel(foreignKey);
		}
	}

	/**
	 * We extend a EntityEditPanel for the department entity,
	 * implementing the initializeUI method.
	 * This is the panel which allows us to edit the attributes
	 * of single department entity instances.
	 */
	public static final class DepartmentEditPanel extends EntityEditPanel {

		public DepartmentEditPanel(SwingEntityEditModel editModel) {
			super(editModel);
		}

		@Override
		protected void initializeUI() {
			focus().initial().set(Department.DEPARTMENT_NO);

			createTextField(Department.DEPARTMENT_NO);
			createTextField(Department.NAME);
			createTextField(Department.LOCATION);

			setLayout(gridLayout(3, 1));

			addInputPanel(Department.DEPARTMENT_NO);
			addInputPanel(Department.NAME);
			addInputPanel(Department.LOCATION);
		}
	}

	/**
	 * We do the same for the employee entity.
	 */
	public static final class EmployeeEditPanel extends EntityEditPanel {

		public EmployeeEditPanel(SwingEntityEditModel editModel) {
			super(editModel);
		}

		@Override
		protected void initializeUI() {
			focus().initial().set(Employee.NAME);

			createTextField(Employee.NAME);
			createComboBox(Employee.DEPARTMENT_FK);
			createTextField(Employee.JOB);
			createComboBox(Employee.MANAGER_FK);
			createTemporalFieldPanel(Employee.HIREDATE);
			createTextField(Employee.SALARY);
			createTextField(Employee.COMMISSION);

			setLayout(gridLayout(4, 2));

			addInputPanel(Employee.NAME);
			addInputPanel(Employee.DEPARTMENT_FK);
			addInputPanel(Employee.JOB);
			addInputPanel(Employee.MANAGER_FK);
			addInputPanel(Employee.HIREDATE);
			addInputPanel(Employee.SALARY);
			addInputPanel(Employee.COMMISSION);
		}
	}

	/**
	 * Then we extend the DefaultEntityApplicationModel class, implementing the
	 * loadDomainModel method, by simply instantiating our domain class, which
	 * initializes the entities it defines.
	 */
	public static final class EmployeesApplicationModel extends SwingEntityApplicationModel {

		private EmployeesApplicationModel(EntityConnectionProvider connectionProvider) {
			super(connectionProvider);
			SwingEntityModel employeeModel = new SwingEntityModel(new EmployeeEditModel(connectionProvider));
			SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider);
			departmentModel.detailModels().add(employeeModel);
			entityModels().add(departmentModel);
		}
	}

	/**
	 * And finally we extend the EntityApplicationPanel class, which is our
	 * main application panel. We implement setupEntityPanelBuilders, in which
	 * we assemble the application from the parts we have defined, and we also
	 * implement the initializeApplicationModel function by returning an instance
	 * of the application model class we defined above.
	 */
	private static final class EmployeesApplicationPanel extends EntityApplicationPanel<EmployeesApplicationModel> {

		private EmployeesApplicationPanel(EmployeesApplicationModel applicationModel) {
			super(applicationModel);
		}

		@Override
		protected List<EntityPanel> createEntityPanels() {
			//now, let's assemble our application
			SwingEntityModel departmentModel = applicationModel().entityModels().get(Department.TYPE);
			SwingEntityModel employeeModel = departmentModel.detailModels().get(Employee.TYPE);

			EntityPanel employeePanel = new EntityPanel(employeeModel,
							new EmployeeEditPanel(employeeModel.editModel()));
			EntityPanel departmentPanel = new EntityPanel(departmentModel,
							new DepartmentEditPanel(departmentModel.editModel()));
			departmentPanel.detailPanels().add(employeePanel);

			return List.of(departmentPanel);
		}
	}

	/*
	 * All that is left is setting the required environment variables and starting the application.
	 */
	public static void main(String[] args) {
		//Let's set the locale, otherwise the application would be in icelandic
		Locale.setDefault(new Locale("en", "EN"));
		//the remote connection settings
		EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
		Clients.SERVER_HOSTNAME.set("codion.is");
		//we're using Secure Sockets Layer so that we need to specify a truststore
		Clients.TRUSTSTORE.set("resources/config/truststore.jks");

		//we create an instance of our application panel and start it
		EntityApplicationPanel.builder(EmployeesApplicationModel.class, EmployeesApplicationPanel.class)
						.applicationName("Employees Minimal")
						.domainType(DOMAIN)
						.defaultLoginUser(User.parse("scott:tiger"))
						.start();
	}
}