/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.minimal;

import org.jminor.common.db.Operator;
import org.jminor.common.model.CancelException;
import org.jminor.common.remote.Server;
import org.jminor.common.user.Users;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.domain.Domain;
import org.jminor.framework.domain.entity.StringProvider;
import org.jminor.framework.domain.property.ForeignKeyProperty;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityComboBoxModel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModelBuilder;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.Types;
import java.util.Locale;

import static java.util.Arrays.asList;
import static org.jminor.framework.domain.entity.KeyGenerators.increment;
import static org.jminor.framework.domain.property.Properties.*;

/**
 * EmpDept minimal application demo
 */
public class EmpDeptMinimalApp {

  /**
   * This class initializes the domain model based on the SCOTT schema
   */
  private static final class EmpDeptDomain extends Domain {

    public EmpDeptDomain() {
      /*
       * We start by defining the entity based on the SCOTT.DEPT table
       */
      define("scott.dept",
              primaryKeyProperty("deptno"),
              columnProperty("dname", Types.VARCHAR, "Department name")
                      .nullable(false)
                      .maximumLength(14),
              columnProperty("loc", Types.VARCHAR, "Department location")
                      .maximumLength(13))
              .keyGenerator(increment("scott.dept", "deptno"))
              .caption("Departments")
              .searchPropertyIds("dname")
              .stringProvider(new StringProvider("dname"));
      /*
       * We then define the entity based on the SCOTT.EMP table,
       * notice the foreign key wrapper properties, referencing the
       * department as well as the manager
       */
      define("scott.emp",
              primaryKeyProperty("empno"),
              columnProperty("ename", Types.VARCHAR, "Name")
                      .nullable(false)
                      .maximumLength(10),
              foreignKeyProperty("dept_fk", "Department", "scott.dept",
                      columnProperty("deptno"))
                      .nullable(false),
              columnProperty("job", Types.VARCHAR, "Job")
                      .nullable(false)
                      .maximumLength(9),
              columnProperty("sal", Types.DOUBLE, "Salary")
                      .nullable(false)
                      .maximumFractionDigits(2)
                      .minimumValue(1000).maximumValue(10000),
              columnProperty("comm", Types.DOUBLE, "Commission")
                      .maximumFractionDigits(2),
              foreignKeyProperty("mgr_fk", "Manager", "scott.emp",
                      columnProperty("mgr")),
              columnProperty("hiredate", Types.DATE, "Hiredate")
                      .nullable(false))
              .keyGenerator(increment("scott.emp", "empno"))
              .caption("Employees")
              .searchPropertyIds("ename")
              .stringProvider(new StringProvider("ename"));
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
      if (foreignKeyProperty.is("mgr_fk")) {
        comboBoxModel.setSelectConditionProvider(() -> Conditions.propertyCondition(
                "job", Operator.LIKE, asList("MANAGER", "PRESIDENT")));
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
      setInitialFocusProperty("dname");

      createTextField("dname");
      createTextField("loc");

      setLayout(new GridLayout(2, 1, 5, 5));

      addPropertyPanel("dname");
      addPropertyPanel("loc");
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
      setInitialFocusProperty("ename");

      createTextField("ename");
      createForeignKeyComboBox("dept_fk");
      createTextField("job");
      createForeignKeyComboBox("mgr_fk");
      createTemporalInputPanel("hiredate");
      createTextField("sal");
      createTextField("comm");

      setLayout(new GridLayout(4, 2, 5, 5));

      addPropertyPanel("ename");
      addPropertyPanel("dept_fk");
      addPropertyPanel("job");
      addPropertyPanel("mgr_fk");
      addPropertyPanel("hiredate");
      addPropertyPanel("sal");
      addPropertyPanel("comm");
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
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set(EmpDeptDomain.class.getName());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
    Server.SERVER_HOST_NAME.set("jminor.no-ip.org");
    //we're using Secure Sockets Layer so we need to specify a truststore
    Server.TRUSTSTORE.set("resources/security/jminor_truststore.jks");
    System.setProperty("java.security.policy", "resources/security/jminor_demos.policy");

    //we create an instance of our application panel and start it
    new EmpDeptApplicationPanel().startApplication("EmpDept Minimal", null, false,
            new Dimension(800, 600), Users.parseUser("scott:tiger"));
  }
}