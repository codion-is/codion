/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
    protected void perform(final EntityConnectionProvider client) throws Exception {
      final List<Entity> departments = client.getConnection().select(condition(Department.TYPE));
      final Entity entity = departments.get(new Random().nextInt(departments.size()));
      entity.put(Department.LOCATION, Text.randomString(10, 13));
      client.getConnection().update(entity);
    }
  }

  private static final class SelectDepartment extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "SelectDepartment";

    private SelectDepartment() {
      super(NAME);
    }

    @Override
    protected void perform(final EntityConnectionProvider client) throws Exception {
      client.getConnection().select(Department.NAME, "ACCOUNTING");
    }
  }

  private static final class SelectEmployees extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "SelectEmployees";

    private SelectEmployees() {
      super(NAME);
    }

    @Override
    protected void perform(final EntityConnectionProvider client) throws Exception {
      final List<Entity> departments = client.getConnection().select(condition(Department.TYPE));

      client.getConnection().select(Employee.DEPARTMENT,
              departments.get(new Random().nextInt(departments.size())).get(Department.ID));
    }
  }

  private static final class AddDepartment extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "AddDepartment";

    private AddDepartment() {
      super(NAME);
    }

    @Override
    protected void perform(final EntityConnectionProvider client) throws Exception {
      final int deptNo = new Random().nextInt(5000);
      client.getConnection().insert(client.getEntities().builder(Department.TYPE)
              .with(Department.ID, deptNo)
              .with(Department.NAME, Text.randomString(4, 8))
              .with(Department.LOCATION, Text.randomString(5, 10))
              .build());
    }
  }

  private static final class AddEmployee extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "AddEmployee";

    private final Random random = new Random();

    private AddEmployee() {
      super(NAME);
    }

    @Override
    protected void perform(final EntityConnectionProvider client) throws Exception {
      final List<Entity> departments = client.getConnection().select(condition(Department.TYPE));
      final Entity department = departments.get(random.nextInt(departments.size()));
      client.getConnection().insert(client.getEntities().builder(Employee.TYPE)
              .with(Employee.DEPARTMENT_FK, department)
              .with(Employee.NAME, Text.randomString(5, 10))
              .with(Employee.JOB, Employee.JOB_VALUES.get(random.nextInt(Employee.JOB_VALUES.size())).getValue())
              .with(Employee.SALARY, BigDecimal.valueOf(random.nextInt(1000) + 1000))
              .with(Employee.HIREDATE, LocalDate.now())
              .with(Employee.COMMISSION, random.nextDouble() * 500)
              .build());
    }
  }
}
