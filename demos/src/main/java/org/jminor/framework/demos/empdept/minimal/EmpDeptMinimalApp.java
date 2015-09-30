/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.minimal;

import org.jminor.common.model.CancelException;
import org.jminor.common.model.SearchType;
import org.jminor.common.model.User;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.criteria.EntityCriteriaUtil;
import org.jminor.framework.domain.Entities;
import org.jminor.framework.domain.Properties;
import org.jminor.framework.domain.Property;
import org.jminor.swing.framework.model.DefaultEntityApplicationModel;
import org.jminor.swing.framework.model.DefaultEntityEditModel;
import org.jminor.swing.framework.model.DefaultEntityModelProvider;
import org.jminor.swing.framework.model.EntityApplicationModel;
import org.jminor.swing.framework.model.EntityComboBoxModel;
import org.jminor.swing.framework.model.EntityEditModel;
import org.jminor.swing.framework.model.EntityModelProvider;
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
  private static final class Domain {

    private Domain() {
      /**
       * We start by defining the entity based on the SCOTT.DEPT table
       */
      Entities.define("scott.dept",
              Properties.primaryKeyProperty("deptno"),
              Properties.columnProperty("dname", Types.VARCHAR, "Department name")
                      .setNullable(false)
                      .setMaxLength(14),
              Properties.columnProperty("loc", Types.VARCHAR, "Department location")
                      .setMaxLength(13))
              .setKeyGenerator(Entities.incrementKeyGenerator("scott.dept", "deptno"))
              .setCaption("Departments")
              .setStringProvider(new Entities.StringProvider("dname"));
      /**
       * We then define the entity based on the SCOTT.EMP table,
       * notice the foreign key wrapper properties, referencing the
       * department as well as the manager
       */
      Entities.define("scott.emp",
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
              .setKeyGenerator(Entities.incrementKeyGenerator("scott.emp", "empno"))
              .setCaption("Employees")
              .setStringProvider(new Entities.StringProvider("ename"));
    }
  }

  /**
   * We extend the default entity edit model to provide a custom
   * combo box model for the manager property
   */
  public static final class EmployeeEditModel extends DefaultEntityEditModel {

    public EmployeeEditModel(final EntityConnectionProvider connectionProvider) {
      super("scott.emp", connectionProvider);
    }

    /**
     * We override this method to add a query criteria to the manager combo box model
     * so that is only shows managers.
     */
    @Override
    public EntityComboBoxModel createEntityComboBoxModel(
            final Property.ForeignKeyProperty foreignKeyProperty) {
      final EntityComboBoxModel comboBoxModel = super.createEntityComboBoxModel(foreignKeyProperty);
      if (foreignKeyProperty.is("mgr_fk")) {
        comboBoxModel.setEntitySelectCriteria(EntityCriteriaUtil.selectCriteria(
                "scott.emp", "job", SearchType.LIKE, Arrays.asList("MANAGER", "PRESIDENT")));
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

    public DepartmentEditPanel(final EntityEditModel editModel) {
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

    public EmployeeEditPanel(final EntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setInitialFocusProperty("ename");

      createTextField("ename");
      createEntityComboBox("dept_fk");
      createTextField("job");
      createEntityComboBox("mgr_fk");
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
  private static final class EmpDeptApplicationModel extends DefaultEntityApplicationModel {

    private EmpDeptApplicationModel(final EntityConnectionProvider connectionProvider) {
      super(connectionProvider);
    }

    @Override
    protected void loadDomainModel() {
      new Domain();
    }
  }

  /**
   * And finally we extend the EntityApplicationPanel class, which is our
   * main application panel. We implement setupEntityPanelProviders, in which
   * we assemble the application from the parts we have defined, and we also
   * implement the initializeApplicationModel function by returning an instance
   * of the application model class we defined above.
   */
  private static final class EmpDeptApplicationPanel extends EntityApplicationPanel {

    @Override
    protected void setupEntityPanelProviders() {
      //now, let's assemble our application
      final EntityPanelProvider departmentProvider = new EntityPanelProvider("scott.dept")
              .setEditPanelClass(DepartmentEditPanel.class);
      final EntityModelProvider employeeModelProvider = new DefaultEntityModelProvider("scott.emp")
              .setEditModelClass(EmployeeEditModel.class);
      final EntityPanelProvider employeeProvider = new EntityPanelProvider(employeeModelProvider)
              .setEditPanelClass(EmployeeEditPanel.class);
      departmentProvider.addDetailPanelProvider(employeeProvider);

      //the department panel is the main (or root) application panel
      addEntityPanelProvider(departmentProvider);
    }

    @Override
    protected EntityApplicationModel initializeApplicationModel(
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
    System.setProperty("jminor.client.connectionType", "remote");
    System.setProperty("jminor.server.hostname", "jminor.no-ip.org");
    System.setProperty("java.security.policy", "resources/security/jminor_demos.policy");
    //we're using Secure Sockets Layer so we need to specify a truststore
    System.setProperty("javax.net.ssl.trustStore", "resources/security/JMinorClientTruststore");

    //we create an instance of our application panel
    final EntityApplicationPanel mainPanel = new EmpDeptApplicationPanel();

    //and then we start the application
    mainPanel.startApplication("EmpDept Minimal", null, false,
            new Dimension(800, 600), new User("scott", "tiger"));
  }
}