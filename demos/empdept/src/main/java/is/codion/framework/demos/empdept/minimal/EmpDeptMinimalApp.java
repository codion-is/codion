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
import is.codion.framework.domain.Domain;
import is.codion.framework.domain.entity.StringProvider;
import is.codion.framework.domain.property.Attribute;
import is.codion.framework.domain.property.EntityAttribute;
import is.codion.framework.domain.property.ForeignKeyProperty;
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

import static is.codion.framework.domain.entity.KeyGenerators.increment;
import static is.codion.framework.domain.property.Attributes.*;
import static is.codion.framework.domain.property.Properties.*;
import static java.util.Arrays.asList;

/**
 * EmpDept minimal application demo
 */
public final class EmpDeptMinimalApp {

  /**
   * This class initializes the domain model based on the SCOTT schema
   */
  private static final class EmpDept extends Domain {

    /*
     * We start by defining attributes for the columns in the SCOTT.DEPT table.
     */
    public static final Attribute<Integer> DEPT_DEPTNO = integerAttribute("deptno");
    public static final Attribute<String> DEPT_DNAME = stringAttribute("dname");
    public static final Attribute<String> DEPT_LOC = stringAttribute("loc");

    /*
     * And for the columns in the SCOTT.EMP table.
     */
    public static final Attribute<Integer> EMP_EMPNO = integerAttribute("empno");
    public static final Attribute<String> EMP_ENAME = stringAttribute("ename");
    public static final Attribute<Integer> EMP_DEPTNO = integerAttribute("deptno");
    public static final EntityAttribute EMP_DEPT_FK = entityAttribute("dept_fk");
    public static final Attribute<String> EMP_JOB = stringAttribute("job");
    public static final Attribute<Double> EMP_SAL = doubleAttribute("sal");
    public static final Attribute<Double> EMP_COMM = doubleAttribute("comm");
    public static final Attribute<Integer> EMP_MGR = integerAttribute("mgr");
    public static final EntityAttribute EMP_MGR_FK = entityAttribute("mgr_fk");
    public static final Attribute<LocalDate> EMP_HIREDATE = localDateAttribute("hiredate");

    public EmpDept() {
      /*
       * We then define the entity based on the SCOTT.DEPT table
       */
      define("scott.dept",
              primaryKeyProperty(DEPT_DEPTNO),
              columnProperty(DEPT_DEPTNO, "Department name")
                      .searchProperty(true)
                      .nullable(false)
                      .maximumLength(14),
              columnProperty(DEPT_LOC, "Department location")
                      .maximumLength(13))
              .keyGenerator(increment("scott.dept", "deptno"))
              .caption("Departments")
              .stringProvider(new StringProvider(DEPT_DNAME));
      /*
       * We then define the entity based on the SCOTT.EMP table,
       * notice the foreign key wrapper properties, referencing the
       * department as well as the manager
       */
      define("scott.emp",
              primaryKeyProperty(EMP_EMPNO),
              columnProperty(EMP_ENAME, "Name")
                      .searchProperty(true)
                      .nullable(false)
                      .maximumLength(10),
              foreignKeyProperty(EMP_DEPT_FK, "Department", "scott.dept",
                      columnProperty(EMP_DEPTNO))
                      .nullable(false),
              columnProperty(EMP_JOB, "Job")
                      .nullable(false)
                      .maximumLength(9),
              columnProperty(EMP_SAL, "Salary")
                      .nullable(false)
                      .maximumFractionDigits(2)
                      .minimumValue(1000).maximumValue(10000),
              columnProperty(EMP_COMM, "Commission")
                      .maximumFractionDigits(2),
              foreignKeyProperty(EMP_MGR_FK, "Manager", "scott.emp",
                      columnProperty(EMP_MGR)),
              columnProperty(EMP_HIREDATE, "Hiredate")
                      .nullable(false))
              .keyGenerator(increment("scott.emp", "empno"))
              .caption("Employees")
              .stringProvider(new StringProvider(EMP_ENAME));
    }
  }

  /**
   * We extend the default entity edit model to provide a custom
   * combo box model for the manager property
   */
  public static final class EmployeeEditModel extends SwingEntityEditModel {

    public EmployeeEditModel(final EntityConnectionProvider connectionProvider) {
      super("scott.emp", connectionProvider);
    }

    /**
     * We override this method to add a query condition to the manager combo box model
     * so that is only shows managers.
     */
    @Override
    public SwingEntityComboBoxModel createForeignKeyComboBoxModel(
            final ForeignKeyProperty foreignKeyProperty) {
      final SwingEntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKeyProperty);
      if (foreignKeyProperty.is(EmpDept.EMP_MGR_FK)) {
        comboBoxModel.setSelectConditionProvider(() -> Conditions.propertyCondition(
                EmpDept.EMP_JOB, Operator.LIKE, asList("MANAGER", "PRESIDENT")));
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
      setInitialFocusAttribute(EmpDept.DEPT_DEPTNO);

      createTextField(EmpDept.DEPT_DNAME);
      createTextField(EmpDept.DEPT_LOC);

      setLayout(new GridLayout(2, 1, 5, 5));

      addPropertyPanel(EmpDept.DEPT_DNAME);
      addPropertyPanel(EmpDept.DEPT_LOC);
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
      setInitialFocusAttribute(EmpDept.EMP_ENAME);

      createTextField(EmpDept.EMP_ENAME);
      createForeignKeyComboBox(EmpDept.EMP_DEPT_FK);
      createTextField(EmpDept.EMP_JOB);
      createForeignKeyComboBox(EmpDept.EMP_MGR_FK);
      createTemporalInputPanel(EmpDept.EMP_HIREDATE);
      createTextField(EmpDept.EMP_SAL);
      createTextField(EmpDept.EMP_COMM);

      setLayout(new GridLayout(4, 2, 5, 5));

      addPropertyPanel(EmpDept.EMP_ENAME);
      addPropertyPanel(EmpDept.EMP_DEPT_FK);
      addPropertyPanel(EmpDept.EMP_JOB);
      addPropertyPanel(EmpDept.EMP_MGR_FK);
      addPropertyPanel(EmpDept.EMP_HIREDATE);
      addPropertyPanel(EmpDept.EMP_SAL);
      addPropertyPanel(EmpDept.EMP_COMM);
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
      final EntityPanelBuilder departmentProvider = new EntityPanelBuilder("scott.dept")
              .setEditPanelClass(DepartmentEditPanel.class);
      final SwingEntityModelBuilder employeeModelBuilder = new SwingEntityModelBuilder("scott.emp")
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