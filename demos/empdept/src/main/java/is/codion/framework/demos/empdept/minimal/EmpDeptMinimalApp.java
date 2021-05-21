/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.minimal;

import is.codion.common.model.CancelException;
import is.codion.common.rmi.client.Clients;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.condition.Conditions;
import is.codion.framework.domain.DefaultDomain;
import is.codion.framework.domain.DomainType;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.domain.entity.EntityType;
import is.codion.framework.domain.entity.ForeignKey;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityComboBoxModel;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.time.LocalDate;
import java.util.Collections;
import java.util.List;
import java.util.Locale;

import static is.codion.framework.domain.DomainType.domainType;
import static is.codion.framework.domain.entity.KeyGenerator.increment;
import static is.codion.framework.domain.entity.StringFactory.stringFactory;
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
    EntityType<Entity> TYPE = DOMAIN.entityType("scott.emp");

    Attribute<Integer> EMPNO = TYPE.integerAttribute("empno");
    Attribute<String> ENAME = TYPE.stringAttribute("ename");
    Attribute<Integer> DEPTNO = TYPE.integerAttribute("deptno");
    Attribute<String> JOB = TYPE.stringAttribute("job");
    Attribute<Double> SAL = TYPE.doubleAttribute("sal");
    Attribute<Double> COMM = TYPE.doubleAttribute("comm");
    Attribute<Integer> MGR = TYPE.integerAttribute("mgr");
    Attribute<LocalDate> HIREDATE = TYPE.localDateAttribute("hiredate");

    ForeignKey DEPT_FK = TYPE.foreignKey("dept_fk", Employee.DEPTNO, Department.DEPTNO);
    ForeignKey MGR_FK = TYPE.foreignKey("mgr_fk", Employee.MGR, Employee.EMPNO);
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
                      .searchProperty()
                      .nullable(false)
                      .maximumLength(14),
              columnProperty(Department.LOC, "Department location")
                      .maximumLength(13))
              .keyGenerator(increment("scott.dept", "deptno"))
              .caption("Departments")
              .stringFactory(stringFactory(Department.DNAME));
      /*
       * We then define the entity based on the SCOTT.EMP table,
       * note the foreign key properties, referencing the
       * department as well as the manager
       */
      define(Employee.TYPE,
              primaryKeyProperty(Employee.EMPNO),
              columnProperty(Employee.ENAME, "Name")
                      .searchProperty()
                      .nullable(false)
                      .maximumLength(10),
              columnProperty(Employee.DEPTNO)
                      .nullable(false),
              foreignKeyProperty(Employee.DEPT_FK, "Department"),
              columnProperty(Employee.JOB, "Job")
                      .nullable(false)
                      .maximumLength(9),
              columnProperty(Employee.SAL, "Salary")
                      .nullable(false)
                      .maximumFractionDigits(2)
                      .minimumValue(1000).maximumValue(10000),
              columnProperty(Employee.COMM, "Commission")
                      .maximumFractionDigits(2),
              columnProperty(Employee.MGR),
              foreignKeyProperty(Employee.MGR_FK, "Manager"),
              columnProperty(Employee.HIREDATE, "Hiredate")
                      .nullable(false))
              .keyGenerator(increment("scott.emp", "empno"))
              .caption("Employees")
              .stringFactory(stringFactory(Employee.ENAME));
    }
  }

  /**
   * We extend the default entity edit model to provide a custom
   * combo box model for the manager property
   */
  public static final class EmployeeEditModel extends SwingEntityEditModel {

    public EmployeeEditModel(final EntityConnectionProvider connectionProvider) {
      super(Employee.TYPE, connectionProvider);
    }

    /**
     * We override this method to add a query condition to the manager combo box model
     * so that is only shows managers.
     */
    @Override
    public SwingEntityComboBoxModel createForeignKeyComboBoxModel(final ForeignKey foreignKey) {
      final SwingEntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKey);
      if (foreignKey.equals(Employee.MGR_FK)) {
        comboBoxModel.setSelectConditionProvider(() ->
                Conditions.condition(Employee.JOB).equalTo("MANAGER", "PRESIDENT"));
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

      textFieldBuilder(Department.DNAME).build();
      textFieldBuilder(Department.LOC).build();

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

      textFieldBuilder(Employee.ENAME).build();
      foreignKeyComboBoxBuilder(Employee.DEPT_FK).build();
      textFieldBuilder(Employee.JOB).build();
      foreignKeyComboBoxBuilder(Employee.MGR_FK).build();
      temporalInputPanelBuilder(Employee.HIREDATE).build();
      textFieldBuilder(Employee.SAL).build();
      textFieldBuilder(Employee.COMM).build();

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
      final SwingEntityModel employeeModel = new SwingEntityModel(new EmployeeEditModel(connectionProvider));
      final SwingEntityModel departmentModel = new SwingEntityModel(Department.TYPE, connectionProvider);
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
  private static final class EmpDeptApplicationPanel extends EntityApplicationPanel<EmpDeptApplicationModel> {

    private EmpDeptApplicationPanel() {
      super("EmpDept Minimal");
    }

    @Override
    protected List<EntityPanel> initializeEntityPanels(final EmpDeptApplicationModel applicationModel) {
      //now, let's assemble our application
      final SwingEntityModel departmentModel = applicationModel.getEntityModel(Department.TYPE);
      final SwingEntityModel employeeModel = departmentModel.getDetailModel(Employee.TYPE);

      final EntityPanel employeePanel = new EntityPanel(employeeModel,
              new EmployeeEditPanel(employeeModel.getEditModel()));
      final EntityPanel departmentPanel = new EntityPanel(departmentModel,
              new DepartmentEditPanel(departmentModel.getEditModel()));
      departmentPanel.addDetailPanel(employeePanel);

      return Collections.singletonList(departmentPanel);
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
    Clients.SERVER_HOST_NAME.set("codion.no-ip.org");
    //we're using Secure Sockets Layer so we need to specify a truststore
    Clients.TRUSTSTORE.set("resources/security/truststore.jks");
    System.setProperty("java.security.policy", "resources/security/codion_demos.policy");

    //we create an instance of our application panel and start it
    SwingUtilities.invokeLater(() -> new EmpDeptApplicationPanel().starter()
            .frameSize(new Dimension(800, 600))
            .defaultLoginUser(User.parseUser("scott:tiger"))
            .start());
  }
}