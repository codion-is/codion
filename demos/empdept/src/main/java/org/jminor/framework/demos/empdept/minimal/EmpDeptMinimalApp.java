/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.minimal;

import org.jminor.common.User;
import org.jminor.common.db.condition.Condition;
import org.jminor.common.model.CancelException;
import org.jminor.common.remote.Server;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.EntityConditions;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;
import org.jminor.framework.model.EntityComboBoxModel;
import org.jminor.swing.framework.model.SwingEntityApplicationModel;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.model.SwingEntityModelProvider;
import org.jminor.swing.framework.ui.EntityApplicationPanel;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.sql.Types;
import java.util.Arrays;
import java.util.Locale;

/**
 * EmpDept minimal application demo
 */
public class EmpDeptMinimalApp {

  /**
   * This class initializes the domain model based on the SCOTT schema
   */
  private static final class Domain extends Entities {

    public Domain() {
      /*
       * We start by defining the entity based on the SCOTT.DEPT table
       */
      define("scott.dept",
              Properties.primaryKeyProperty("deptno"),
              Properties.columnProperty("dname", Types.VARCHAR, "Department name")
                      .setNullable(false)
                      .setMaxLength(14),
              Properties.columnProperty("loc", Types.VARCHAR, "Department location")
                      .setMaxLength(13))
              .setKeyGenerator(incrementKeyGenerator("scott.dept", "deptno"))
              .setCaption("Departments")
              .setSearchPropertyIds("dname")
              .setStringProvider(new Entities.StringProvider("dname"));
      /*
       * We then define the entity based on the SCOTT.EMP table,
       * notice the foreign key wrapper properties, referencing the
       * department as well as the manager
       */
      define("scott.emp",
              Properties.primaryKeyProperty("empno"),
              Properties.columnProperty("ename", Types.VARCHAR, "Name")
                      .setNullable(false)
                      .setMaxLength(10),
              Properties.foreignKeyProperty("dept_fk", "Department", "scott.dept",
                      Properties.columnProperty("deptno"))
                      .setNullable(false),
              Properties.columnProperty("job", Types.VARCHAR, "Job")
                      .setNullable(false)
                      .setMaxLength(9),
              Properties.columnProperty("sal", Types.DOUBLE, "Salary")
                      .setNullable(false)
                      .setMaximumFractionDigits(2)
                      .setMin(1000).setMax(10000),
              Properties.columnProperty("comm", Types.DOUBLE, "Commission")
                      .setMaximumFractionDigits(2),
              Properties.foreignKeyProperty("mgr_fk", "Manager", "scott.emp",
                      Properties.columnProperty("mgr")),
              Properties.columnProperty("hiredate", Types.DATE, "Hiredate")
                      .setNullable(false))
              .setKeyGenerator(incrementKeyGenerator("scott.emp", "empno"))
              .setCaption("Employees")
              .setSearchPropertyIds("ename")
              .setStringProvider(new Entities.StringProvider("ename"));
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
    public EntityComboBoxModel createForeignKeyComboBoxModel(
            final Property.ForeignKeyProperty foreignKeyProperty) {
      final EntityComboBoxModel comboBoxModel = super.createForeignKeyComboBoxModel(foreignKeyProperty);
      if (foreignKeyProperty.is("mgr_fk")) {
        comboBoxModel.setSelectConditionProvider(() -> new EntityConditions(getDomain()).propertyCondition(
                "scott.emp", "job", Condition.Type.LIKE, Arrays.asList("MANAGER", "PRESIDENT")));
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
      createDateInputPanel("hiredate");
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
   * main application panel. We implement setupEntityPanelProviders, in which
   * we assemble the application from the parts we have defined, and we also
   * implement the initializeApplicationModel function by returning an instance
   * of the application model class we defined above.
   */
  private static final class EmpDeptApplicationPanel extends EntityApplicationPanel<EmpDeptApplicationModel> {

    @Override
    protected void setupEntityPanelProviders() {
      //now, let's assemble our application
      final EntityPanelProvider departmentProvider = new EntityPanelProvider("scott.dept",
              getModel().getDomain().getCaption("scott.dept"))
              .setEditPanelClass(DepartmentEditPanel.class);
      final SwingEntityModelProvider employeeModelProvider = new SwingEntityModelProvider("scott.emp")
              .setEditModelClass(EmployeeEditModel.class);
      final EntityPanelProvider employeeProvider = new EntityPanelProvider(employeeModelProvider,
              getModel().getDomain().getCaption("scott.emp"))
              .setEditPanelClass(EmployeeEditPanel.class);
      departmentProvider.addDetailPanelProvider(employeeProvider);

      //the department panel is the main (or root) application panel
      addEntityPanelProvider(departmentProvider);
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
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set(Domain.class.getName());
    EntityConnectionProvider.CLIENT_CONNECTION_TYPE.set(EntityConnectionProvider.CONNECTION_TYPE_REMOTE);
    Server.SERVER_HOST_NAME.set("jminor.no-ip.org");
    //we're using Secure Sockets Layer so we need to specify a truststore
    Server.TRUSTSTORE.set("resources/security/jminor_truststore.jks");
    System.setProperty("java.security.policy", "resources/security/jminor_demos.policy");

    //we create an instance of our application panel and start it
    new EmpDeptApplicationPanel().startApplication("EmpDept Minimal", null, false,
            new Dimension(800, 600), new User("scott", "tiger".toCharArray()));
  }
}