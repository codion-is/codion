/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.empdept.rest;

import org.jminor.common.TextUtil;
import org.jminor.common.User;
import org.jminor.common.model.CancelException;
import org.jminor.framework.db.EntityConnectionProvider;
import org.jminor.framework.db.condition.Conditions;
import org.jminor.framework.db.http.HttpEntityConnectionProvider;
import org.jminor.framework.demos.empdept.domain.EmpDept;
import org.jminor.framework.domain.Entity;
import org.jminor.swing.common.tools.LoadTestModel;
import org.jminor.swing.common.tools.ui.LoadTestPanel;

import javax.swing.UIManager;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

public final class EmpDeptServletLoadTest extends LoadTestModel<EntityConnectionProvider> {

  private static final User UNIT_TEST_USER = new User(
          System.getProperty("jminor.unittest.username", "scott"),
          System.getProperty("jminor.unittest.password", "tiger").toCharArray());

  public EmpDeptServletLoadTest(final User user) {
    super(user, asList(new SelectDepartment(), new UpdateLocation(), new SelectEmployees(), new AddDepartment(), new AddEmployee()),
            5000, 2, 10, 500);
    setWeight(UpdateLocation.NAME, 2);
    setWeight(SelectDepartment.NAME, 4);
    setWeight(SelectEmployees.NAME, 5);
    setWeight(AddDepartment.NAME, 1);
    setWeight(AddEmployee.NAME, 4);
  }

  @Override
  protected void disconnectApplication(final EntityConnectionProvider client) {
    client.disconnect();
  }

  @Override
  protected EntityConnectionProvider initializeApplication() throws CancelException {
    return new HttpEntityConnectionProvider(
            HttpEntityConnectionProvider.HTTP_CLIENT_HOST_NAME.get(),
            HttpEntityConnectionProvider.HTTP_CLIENT_PORT.get(),
            HttpEntityConnectionProvider.HTTP_CLIENT_SECURE.get())
            .setClientTypeId("EmpDeptServletLoadTest")
            .setDomainClassName(EmpDept.class.getName()).setUser(UNIT_TEST_USER);
  }

  public static void main(final String[] args) throws Exception {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
    new LoadTestPanel(new EmpDeptServletLoadTest(UNIT_TEST_USER)).showFrame();
  }

  private static final class UpdateLocation extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "UpdateLocation";

    private UpdateLocation() {
      super(NAME);
    }

    @Override
    protected void performScenario(final EntityConnectionProvider client) throws ScenarioException {
      try {
        final List<Entity> departments = client.getConnection().selectMany(Conditions.entitySelectCondition(EmpDept.T_DEPARTMENT));
        final Entity entity = departments.get(new Random().nextInt(departments.size()));
        entity.put(EmpDept.DEPARTMENT_LOCATION, TextUtil.createRandomString(10, 13));
        client.getConnection().update(singletonList(entity));
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }
  }

  private static final class SelectDepartment extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "SelectDepartment";

    private SelectDepartment() {
      super(NAME);
    }

    @Override
    protected void performScenario(final EntityConnectionProvider client) throws ScenarioException {
      try {
        client.getConnection().selectMany(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "ACCOUNTING");
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }
  }

  private static final class SelectEmployees extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "SelectEmployees";

    private SelectEmployees() {
      super(NAME);
    }

    @Override
    protected void performScenario(final EntityConnectionProvider client) throws ScenarioException {
      try {
        final List<Entity> departments = client.getConnection().selectMany(Conditions.entitySelectCondition(EmpDept.T_DEPARTMENT));

        client.getConnection().selectMany(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT,
                departments.get(new Random().nextInt(departments.size())).getAsString(EmpDept.DEPARTMENT_ID));
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }
  }

  private static final class AddDepartment extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "AddDepartment";

    private AddDepartment() {
      super(NAME);
    }

    @Override
    protected void performScenario(final EntityConnectionProvider client) throws ScenarioException {
      try {
        final int deptNo = new Random().nextInt(5000);
        final Entity department = client.getDomain().entity(EmpDept.T_DEPARTMENT);
        department.put(EmpDept.DEPARTMENT_ID, deptNo);
        department.put(EmpDept.DEPARTMENT_NAME, TextUtil.createRandomString(4, 8));
        department.put(EmpDept.DEPARTMENT_LOCATION, TextUtil.createRandomString(5, 10));

        client.getConnection().insert(singletonList(department));
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }
  }

  private static final class AddEmployee extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "AddEmployee";

    private final Random random = new Random();

    private AddEmployee() {
      super(NAME);
    }

    @Override
    protected void performScenario(final EntityConnectionProvider client) throws ScenarioException {
      try {
        final List<Entity> departments = client.getConnection().selectMany(Conditions.entitySelectCondition(EmpDept.T_DEPARTMENT));
        final Entity department = departments.get(random.nextInt(departments.size()));
        final Entity employee = client.getDomain().entity(EmpDept.T_EMPLOYEE);
        employee.put(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
        employee.put(EmpDept.EMPLOYEE_NAME, TextUtil.createRandomString(5, 10));
        employee.put(EmpDept.EMPLOYEE_JOB, EmpDept.JOB_VALUES.get(random.nextInt(EmpDept.JOB_VALUES.size())).getValue());
        employee.put(EmpDept.EMPLOYEE_SALARY, BigDecimal.valueOf(random.nextInt(1000) + 1000));
        employee.put(EmpDept.EMPLOYEE_HIREDATE, LocalDate.now());
        employee.put(EmpDept.EMPLOYEE_COMMISSION, random.nextDouble() * 500);

        client.getConnection().insert(singletonList(employee));
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }
  }
}
