/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.minimal;

import is.codion.common.db.Operator;
import is.codion.common.model.CancelException;
import is.codion.common.rmi.server.ServerConfiguration;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModelBuilder;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityApplicationPanel.MaximizeFrame;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.Locale;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.property.Properties.*;

/**
 * EmpDept minimal application demo
 */
public final class EmpDeptMinimalApp {

  /*
   * The domain model identifier.
   */
  static final DomainType DOMAIN = domainType(EmpDeptMinimalApp.class);

  /*
   * We start by defining attributes for the columns in the SCOTT.DEPT table.
   */
  interface Department {
    EntityType<Entity> TYPE = DOMAIN.entityType("scott.dept");
    Attribute<Integer> DEPTNO = TYPE.integerAttribute("deptno");
    Attribute<String> DNAME = TYPE.stringAttribute("dname");
    Attribute<String> LOC = TYPE.stringAttribute("loc");
  }

  /*
   * And for the columns in the SCOTT.EMP table.
   */
  interface Employee {
    EntityType<Entity> T_EMP = DOMAIN.entityType("scott.emp");
    Attribute<Integer> EMPNO = T_EMP.integerAttribute("empno");
    Attribute<String> ENAME = T_EMP.stringAttribute("ename");
    Attribute<Integer> DEPTNO = T_EMP.integerAttribute("deptno");
    Attribute<Entity> DEPT_FK = T_EMP.entityAttribute("dept_fk");
    Attribute<String> JOB = T_EMP.stringAttribute("job");
    Attribute<Double> SAL = T_EMP.doubleAttribute("sal");
    Attribute<Double> COMM = T_EMP.doubleAttribute("comm");
    Attribute<Integer> MGR = T_EMP.integerAttribute("mgr");
    Attribute<Entity> MGR_FK = T_EMP.entityAttribute("mgr_fk");
    Attribute<LocalDate> HIREDATE = T_EMP.localDateAttribute("hiredate");
  }

  /**
   * This class initializes the domain model based on the SCOTT schema
   */
  private static final class EmpDept extends DefaultDomain {

    public EmpDept() {
      super(DOMAIN);
      /*
       * We then define the entity based on the SCOTT.DEPT table
       */
      define(Department.TYPE,
              primaryKeyProperty(Department.DEPTNO),
              columnProperty(Department.DEPTNO, "Department name")
                      .searchProperty(true)
                      .nullable(false)
                      .maximumLength(14),
              columnProperty(Department.LOC, "Department location")
                      .maximumLength(13))
              .keyGenerator(increment("scott.dept", "deptno"))
              .caption("Departments")
              .stringProvider(new StringProvider(Department.DNAME));
      /*
       * We then define the entity based on the SCOTT.EMP table,
       * notice the foreign key wrapper properties, referencing the
       * department as well as the manager
       */
      define(Employee.T_EMP,
              primaryKeyProperty(Employee.EMPNO),
              columnProperty(Employee.ENAME, "Name")
                      .searchProperty(true)
                      .nullable(false)
                      .maximumLength(10),
              foreignKeyProperty(Employee.DEPT_FK, "Department", Department.TYPE,
                      columnProperty(Employee.DEPTNO))
                      .nullable(false),
              columnProperty(Employee.JOB, "Job")
                      .nullable(false)
                      .maximumLength(9),
              columnProperty(Employee.SAL, "Salary")
                      .nullable(false)
                      .maximumFractionDigits(2)
                      .minimumValue(1000).maximumValue(10000),
              columnProperty(Employee.COMM, "Commission")
                      .maximumFractionDigits(2),
              foreignKeyProperty(Employee.MGR_FK, "Manager", Employee.T_EMP,
                      columnProperty(Employee.MGR)),
              columnProperty(Employee.HIREDATE, "Hiredate")
                      .nullable(false))
              .keyGenerator(increment("scott.emp", "empno"))
              .caption("Employees")
              .stringProvider(new StringProvider(Employee.ENAME));
    }
  }

  /**
   * We extend the default entity edit model to provide a custom
   * combo box model for the manager property
   */
  public static final class EmployeeEditModel extends SwingEntityEditModel {

    public EmployeeEditModel(final EntityConnectionProvider connectionProvider) {
      super(Employee.T_EMP, connectionProvider);
    }

    /**
     * We override this method to add a query condition to the manager combo box model
     * so that is only shows managers.
     */
    @Override
    public SwingEntityComboBoxModel createForeignKeyComboBoxModel(
            final Attribute<Entity> foreignKeyAttribute) {
      final SwingEntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKeyAttribute);
      if (foreignKeyAttribute.equals(Employee.MGR_FK)) {
        comboBoxModel.setSelectConditionProvider(() ->
                Conditions.condition(Employee.JOB, Operator.EQUAL_TO, "MANAGER", "PRESIDENT"));
        comboBoxModel.refresh();
      }

      return comboBoxModel;
    }
  }

  /**
   * We extend a EntityEditPanel for the department entity,
   * implementing the initializeUI method.
   * This is the panel which allows us to edit the properties
   * of single department entity instances.
   */
  public static final class DepartmentEditPanel extends EntityEditPanel {

    public DepartmentEditPanel(final SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusAttribute(Department.DEPTNO);

      createTextField(Department.DNAME);
      createTextField(Department.LOC);

      setLayout(new GridLayout(2, 1, 5, 5));

      addInputPanel(Department.DNAME);
      addInputPanel(Department.LOC);
    }
  }

  /**
   * We do the same for the employee entity.
   */
  public static final class EmployeeEditPanel extends EntityEditPanel {

    public EmployeeEditPanel(final SwingEntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusAttribute(Employee.ENAME);

      createTextField(Employee.ENAME);
      createForeignKeyComboBox(Employee.DEPT_FK);
      createTextField(Employee.JOB);
      createForeignKeyComboBox(Employee.MGR_FK);
      createTemporalInputPanel(Employee.HIREDATE);
      createTextField(Employee.SAL);
      createTextField(Employee.COMM);

      setLayout(new GridLayout(4, 2, 5, 5));

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
  public static final class EmpDeptApplicationModel extends SwingEntityApplicationModel {

    private EmpDeptApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }
  }

  /**
   * And finally we extend the EntityApplicationPanel class, which is our
   * main application panel. We implement setupEntityPanelBuilders, in which
   * we assemble the application from the parts we have defined, and we also
   * implement the initializeApplicationModel function by returning an instance
   * of the application model class we defined above.
   */
  private static final class EmpDeptApplicationPanel extends EntityApplicationPanel<EmpDeptApplicationModel> {

    @Override
    protected void setupEntityPanelBuilders() {
      //now, let's assemble our application
      final EntityPanelBuilder departmentProvider = new EntityPanelBuilder(Department.TYPE)
              .setEditPanelClass(DepartmentEditPanel.class);
      final SwingEntityModelBuilder employeeModelBuilder = new SwingEntityModelBuilder(Employee.T_EMP)
              .setEditModelClass(EmployeeEditModel.class);
      final EntityPanelBuilder employeeProvider = new EntityPanelBuilder(employeeModelBuilder)
              .setEditPanelClass(EmployeeEditPanel.class);
      departmentProvider.addDetailPanelBuilder(employeeProvider);

      //the department panel is the main (or root) application panel
      addEntityPanelBuilder(departmentProvider);
    }

    @Override
    protected EmpDeptApplicationModel initializeApplicationModel(
            final EntityConnectionProvider connectionProvider) throws CancelException {
      return new EmpDeptApplicationModel(connectionProvider);
    }
  }

  /*
   * All that is left is setting the required environment variables and starting the application.
   */
  public static void main(final String[] args) {
    //Let's set the locale, otherwise the application would be in icelandic
    Locale.setDefault(new Locale("en", "EN"));
    //the remote connection settings
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set(EmpDept.class.getName());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
    ServerConfiguration.SERVER_HOST_NAME.set("codion.no-ip.org");
    //we're using Secure Sockets Layer so we need to specify a truststore
    ServerConfiguration.TRUSTSTORE.set("resources/security/truststore.jks");
    System.setProperty("java.security.policy", "resources/security/codion_demos.policy");

    //we create an instance of our application panel and start it
    new EmpDeptApplicationPanel().startApplication("EmpDept Minimal", null, MaximizeFrame.NO,
            new Dimension(800, 600), Users.parseUser("scott:tiger"));
  }
}