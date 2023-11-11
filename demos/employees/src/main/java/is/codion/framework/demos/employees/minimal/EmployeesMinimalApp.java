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
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.employees.minimal;

import is.codion.common.rmi.client.Clients;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.domain.DefaultDomain;
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

import java.awt.Dimension;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.sequence;
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
   * We start by defining attributes for the columns in the SCOTT.DEPT table.
   */
  interface Department {
    EntityType TYPE = DOMAIN.entityType("scott.dept");

    Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
    Column<String> DNAME = TYPE.stringColumn("dname");
    Column<String> LOC = TYPE.stringColumn("loc");
  }

  /*
   * And for the columns in the SCOTT.EMP table.
   */
  interface Employee {
    EntityType TYPE = DOMAIN.entityType("scott.emp");

    Column<Integer> ID = TYPE.integerColumn("id");
    Column<String> ENAME = TYPE.stringColumn("ename");
    Column<Integer> DEPTNO = TYPE.integerColumn("deptno");
    Column<String> JOB = TYPE.stringColumn("job");
    Column<Double> SAL = TYPE.doubleColumn("sal");
    Column<Double> COMM = TYPE.doubleColumn("comm");
    Column<Integer> MGR = TYPE.integerColumn("mgr");
    Column<LocalDate> HIREDATE = TYPE.localDateColumn("hiredate");

    ForeignKey DEPT_FK = TYPE.foreignKey("dept_fk", DEPTNO, Department.DEPTNO);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", MGR, Employee.ID);
  }

  /**
   * This class initializes the domain model based on the SCOTT schema
   */
  private static final class Employees extends DefaultDomain {

    public Employees() {
      super(DOMAIN);
      /*
       * We then define the entity based on the SCOTT.DEPT table
       */
      add(Department.TYPE.define(
              Department.DEPTNO.define()
                      .primaryKey(),
              Department.DEPTNO.define()
                      .column()
                      .caption("Department name")
                      .searchColumn(true)
                      .nullable(false)
                      .maximumLength(14),
             Department.LOC.define()
                     .column()
                     .caption("Department location")
                     .maximumLength(13))
              .keyGenerator(sequence("scott.dept_seq"))
              .caption("Departments")
              .stringFactory(Department.DNAME));
      /*
       * We then define the entity based on the SCOTT.EMP table,
       * note the foreign keys, referencing the
       * department as well as the manager
       */
      add(Employee.TYPE.define(
              Employee.ID.define()
                      .primaryKey(),
              Employee.ENAME.define()
                      .column()
                      .caption("Name")
                      .searchColumn(true)
                      .nullable(false)
                      .maximumLength(10),
              Employee.DEPTNO.define()
                      .column()
                      .nullable(false),
              Employee.DEPT_FK.define()
                      .foreignKey()
                      .caption("Department"),
              Employee.JOB.define()
                      .column()
                      .caption("Job")
                      .nullable(false)
                      .maximumLength(9),
              Employee.SAL.define()
                      .column()
                      .caption("Salary")
                      .nullable(false)
                      .maximumFractionDigits(2)
                      .valueRange(1000, 10000),
              Employee.COMM.define()
                      .column()
                      .caption("Commission")
                      .maximumFractionDigits(2),
              Employee.MGR.define()
                      .column(),
              Employee.MGR_FK.define()
                      .foreignKey()
                      .caption("Manager"),
              Employee.HIREDATE.define()
                      .column()
                      .caption("Hiredate")
                      .nullable(false))
            .keyGenerator(KeyGenerator.sequence("scott.emp_seq"))
            .caption("Employees")
            .stringFactory(Employee.ENAME));
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
      initializeComboBoxModels(Employee.MGR_FK, Employee.DEPT_FK);
    }

    /**
     * We override this method to add a query condition to the manager combo box model
     * so that is only shows managers.
     */
    @Override
    public EntityComboBoxModel createForeignKeyComboBoxModel(ForeignKey foreignKey) {
      EntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKey);
      if (foreignKey.equals(Employee.MGR_FK)) {
        comboBoxModel.condition().set(() ->
                Employee.JOB.in("Manager", "President"));
      }

      return comboBoxModel;
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
      initialFocusAttribute().set(Department.DEPTNO);

      createTextField(Department.DEPTNO);
      createTextField(Department.DNAME);
      createTextField(Department.LOC);

      setLayout(gridLayout(3, 1));

      addInputPanel(Department.DEPTNO);
      addInputPanel(Department.DNAME);
      addInputPanel(Department.LOC);
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
      initialFocusAttribute().set(Employee.ENAME);

      createTextField(Employee.ENAME);
      createForeignKeyComboBox(Employee.DEPT_FK);
      createTextField(Employee.JOB);
      createForeignKeyComboBox(Employee.MGR_FK);
      createTemporalInputPanel(Employee.HIREDATE);
      createTextField(Employee.SAL);
      createTextField(Employee.COMM);

      setLayout(gridLayout(4, 2));

      addInputPanel(Employee.ENAME);
      addInputPanel(Employee.DEPT_FK);
      addInputPanel(Employee.JOB);
      addInputPanel(Employee.MGR_FK);
      addInputPanel(Employee.HIREDATE);
      addInputPanel(Employee.SAL);
      addInputPanel(Employee.COMM);
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
      departmentModel.addDetailModel(employeeModel);
      addEntityModel(departmentModel);
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
      SwingEntityModel departmentModel = applicationModel().entityModel(Department.TYPE);
      SwingEntityModel employeeModel = departmentModel.detailModel(Employee.TYPE);

      EntityPanel employeePanel = new EntityPanel(employeeModel,
              new EmployeeEditPanel(employeeModel.editModel()));
      EntityPanel departmentPanel = new EntityPanel(departmentModel,
              new DepartmentEditPanel(departmentModel.editModel()));
      departmentPanel.addDetailPanel(employeePanel);

      return Collections.singletonList(departmentPanel);
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
            .frameSize(new Dimension(800, 600))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
}