/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.rest;

import is.codion.common.Text;
import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.common.user.Users;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.tools.loadtest.AbstractUsageScenario;
import is.codion.swing.common.tools.loadtest.LoadTestModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.common.tools.ui.loadtest.LoadTestPanel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static is.codion.framework.db.condition.Conditions.selectCondition;
import static java.util.Arrays.asList;

public final class EmpDeptServletLoadTest extends LoadTestModel<EntityConnectionProvider> {

  private static final User UNIT_TEST_USER =
          Users.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

  public EmpDeptServletLoadTest(final User user) {
    super(user, asList(new SelectDepartment(), new UpdateLocation(), new SelectEmployees(), new AddDepartment(), new AddEmployee()),
            5000, 2, 10);
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
    new LoadTestPanel(new EmpDeptServletLoadTest(UNIT_TEST_USER)).showFrame();
  }

  private static final class UpdateLocation extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "UpdateLocation";

    private UpdateLocation() {
      super(NAME);
    }

    @Override
    protected void perform(final EntityConnectionProvider client) throws ScenarioException {
      try {
        final List<Entity> departments = client.getConnection().select(selectCondition(EmpDept.T_DEPARTMENT));
        final Entity entity = departments.get(new Random().nextInt(departments.size()));
        entity.put(EmpDept.DEPARTMENT_LOCATION, Text.createRandomString(10, 13));
        client.getConnection().update(entity);
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
    protected void perform(final EntityConnectionProvider client) throws ScenarioException {
      try {
        client.getConnection().select(EmpDept.T_DEPARTMENT, EmpDept.DEPARTMENT_NAME, "ACCOUNTING");
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
    protected void perform(final EntityConnectionProvider client) throws ScenarioException {
      try {
        final List<Entity> departments = client.getConnection().select(selectCondition(EmpDept.T_DEPARTMENT));

        client.getConnection().select(EmpDept.T_EMPLOYEE, EmpDept.EMPLOYEE_DEPARTMENT,
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
    protected void perform(final EntityConnectionProvider client) throws ScenarioException {
      try {
        final int deptNo = new Random().nextInt(5000);
        final Entity department = client.getEntities().entity(EmpDept.T_DEPARTMENT);
        department.put(EmpDept.DEPARTMENT_ID, deptNo);
        department.put(EmpDept.DEPARTMENT_NAME, Text.createRandomString(4, 8));
        department.put(EmpDept.DEPARTMENT_LOCATION, Text.createRandomString(5, 10));

        client.getConnection().insert(department);
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
    protected void perform(final EntityConnectionProvider client) throws ScenarioException {
      try {
        final List<Entity> departments = client.getConnection().select(selectCondition(EmpDept.T_DEPARTMENT));
        final Entity department = departments.get(random.nextInt(departments.size()));
        final Entity employee = client.getEntities().entity(EmpDept.T_EMPLOYEE);
        employee.put(EmpDept.EMPLOYEE_DEPARTMENT_FK, department);
        employee.put(EmpDept.EMPLOYEE_NAME, Text.createRandomString(5, 10));
        employee.put(EmpDept.EMPLOYEE_JOB, EmpDept.JOB_VALUES.get(random.nextInt(EmpDept.JOB_VALUES.size())).getValue());
        employee.put(EmpDept.EMPLOYEE_SALARY, BigDecimal.valueOf(random.nextInt(1000) + 1000));
        employee.put(EmpDept.EMPLOYEE_HIREDATE, LocalDate.now());
        employee.put(EmpDept.EMPLOYEE_COMMISSION, random.nextDouble() * 500);

        client.getConnection().insert(employee);
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }
  }
}
