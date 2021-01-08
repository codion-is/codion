/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.rest;

import is.codion.common.Text;
import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.demos.empdept.domain.EmpDept;
import is.codion.framework.demos.empdept.domain.EmpDept.Department;
import is.codion.framework.demos.empdept.domain.EmpDept.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.tools.loadtest.AbstractUsageScenario;
import is.codion.swing.common.tools.loadtest.LoadTestModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.common.tools.ui.loadtest.LoadTestPanel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static is.codion.framework.db.condition.Conditions.condition;
import static java.util.Arrays.asList;

public final class EmpDeptServletLoadTest extends LoadTestModel<EntityConnectionProvider> {

  private static final User UNIT_TEST_USER =
          User.parseUser(System.getProperty("codion.test.user", "scott:tiger"));

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
    client.close();
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
    new LoadTestPanel<>(new EmpDeptServletLoadTest(UNIT_TEST_USER)).showFrame();
  }

  private static final class UpdateLocation extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "UpdateLocation";

    private UpdateLocation() {
      super(NAME);
    }

    @Override
    protected void perform(final EntityConnectionProvider client) throws ScenarioException {
      try {
        final List<Entity> departments = client.getConnection().select(condition(Department.TYPE));
        final Entity entity = departments.get(new Random().nextInt(departments.size()));
        entity.put(Department.LOCATION, Text.createRandomString(10, 13));
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
        client.getConnection().select(Department.NAME, "ACCOUNTING");
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
        final List<Entity> departments = client.getConnection().select(condition(Department.TYPE));

        client.getConnection().select(Employee.DEPARTMENT,
                departments.get(new Random().nextInt(departments.size())).get(Department.ID));
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
        final Entity department = client.getEntities().entity(Department.TYPE);
        department.put(Department.ID, deptNo);
        department.put(Department.NAME, Text.createRandomString(4, 8));
        department.put(Department.LOCATION, Text.createRandomString(5, 10));

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
        final List<Entity> departments = client.getConnection().select(condition(Department.TYPE));
        final Entity department = departments.get(random.nextInt(departments.size()));
        final Entity employee = client.getEntities().entity(Employee.TYPE);
        employee.put(Employee.DEPARTMENT_FK, department);
        employee.put(Employee.NAME, Text.createRandomString(5, 10));
        employee.put(Employee.JOB, Employee.JOB_VALUES.get(random.nextInt(Employee.JOB_VALUES.size())).getValue());
        employee.put(Employee.SALARY, BigDecimal.valueOf(random.nextInt(1000) + 1000));
        employee.put(Employee.HIREDATE, LocalDate.now());
        employee.put(Employee.COMMISSION, random.nextDouble() * 500);

        client.getConnection().insert(employee);
      }
      catch (final Exception e) {
        throw new ScenarioException(e);
      }
    }
  }
}
