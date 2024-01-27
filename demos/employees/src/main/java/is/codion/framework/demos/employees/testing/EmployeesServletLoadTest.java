/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing;

import is.codion.common.Text;
import is.codion.common.model.CancelException;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;
import is.codion.swing.common.model.tools.loadtest.LoadTestModel;
import is.codion.swing.common.ui.tools.loadtest.LoadTestPanel;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Random;

import static is.codion.framework.domain.entity.condition.Condition.all;
import static java.util.Arrays.asList;

public final class EmployeesServletLoadTest {

  private static final User UNIT_TEST_USER =
          User.parse(System.getProperty("codion.test.user", "scott:tiger"));

  private final LoadTestModel<EntityConnectionProvider> loadTestModel;

  private EmployeesServletLoadTest(User user) {
    loadTestModel = LoadTestModel.builder(EmployeesServletLoadTest::createApplication, EmployeesServletLoadTest::disconnectApplication)
            .user(user)
            .usageScenarios(asList(new SelectDepartment(), new UpdateLocation(), new SelectEmployees(), new AddDepartment(), new AddEmployee()))
            .minimumThinkTime(2500)
            .maximumThinkTime(5000)
            .loginDelayFactor(2)
            .applicationBatchSize(10)
            .build();
    loadTestModel.setWeight(UpdateLocation.NAME, 2);
    loadTestModel.setWeight(SelectDepartment.NAME, 4);
    loadTestModel.setWeight(SelectEmployees.NAME, 5);
    loadTestModel.setWeight(AddDepartment.NAME, 1);
    loadTestModel.setWeight(AddEmployee.NAME, 4);
  }

  private static void disconnectApplication(EntityConnectionProvider client) {
    client.close();
  }

  private static EntityConnectionProvider createApplication(User user) throws CancelException {
    return HttpEntityConnectionProvider.builder()
            .clientTypeId("EmployeesServletLoadTest")
            .domainType(Employees.DOMAIN)
            .user(user)
            .build();
  }

  public static void main(String[] args) {
    new LoadTestPanel<>(new EmployeesServletLoadTest(UNIT_TEST_USER).loadTestModel).run();
  }

  private static final class UpdateLocation extends AbstractUsageScenario<EntityConnectionProvider> {

    private static final String NAME = "UpdateLocation";

    private UpdateLocation() {
      super(NAME);
    }

    @Override
    protected void perform(EntityConnectionProvider client) throws Exception {
      List<Entity> departments = client.connection().select(all(Department.TYPE));
      Entity entity = departments.get(new Random().nextInt(departments.size()));
      entity.put(Department.LOCATION, Text.randomString(10, 13));
      client.connection().update(entity);
    }
  }

  private static final class SelectDepartment extends AbstractUsageScenario<EntityConnectionProvider> {

    private static final String NAME = "SelectDepartment";

    private SelectDepartment() {
      super(NAME);
    }

    @Override
    protected void perform(EntityConnectionProvider client) throws Exception {
      client.connection().select(Department.NAME.equalTo("Accounting"));
    }
  }

  private static final class SelectEmployees extends AbstractUsageScenario<EntityConnectionProvider> {

    private static final String NAME = "SelectEmployees";

    private SelectEmployees() {
      super(NAME);
    }

    @Override
    protected void perform(EntityConnectionProvider client) throws Exception {
      List<Entity> departments = client.connection().select(all(Department.TYPE));

      client.connection().select(Employee.DEPARTMENT
              .equalTo(departments.get(new Random().nextInt(departments.size())).get(Department.DEPARTMENT_NO)));
    }
  }

  private static final class AddDepartment extends AbstractUsageScenario<EntityConnectionProvider> {
    public static final String NAME = "AddDepartment";

    private AddDepartment() {
      super(NAME);
    }

    @Override
    protected void perform(EntityConnectionProvider client) throws Exception {
      int departmentNo = new Random().nextInt(5000);
      client.connection().insert(client.entities().builder(Department.TYPE)
              .with(Department.DEPARTMENT_NO, departmentNo)
              .with(Department.NAME, Text.randomString(4, 8))
              .with(Department.LOCATION, Text.randomString(5, 10))
              .build());
    }
  }

  private static final class AddEmployee extends AbstractUsageScenario<EntityConnectionProvider> {

    private static final String NAME = "AddEmployee";

    private final Random random = new Random();

    private AddEmployee() {
      super(NAME);
    }

    @Override
    protected void perform(EntityConnectionProvider client) throws Exception {
      List<Entity> departments = client.connection().select(all(Department.TYPE));
      Entity department = departments.get(random.nextInt(departments.size()));
      client.connection().insert(client.entities().builder(Employee.TYPE)
              .with(Employee.DEPARTMENT_FK, department)
              .with(Employee.NAME, Text.randomString(5, 10))
              .with(Employee.JOB, Employee.JOB_VALUES.get(random.nextInt(Employee.JOB_VALUES.size())).get())
              .with(Employee.SALARY, BigDecimal.valueOf(random.nextInt(1000) + 1000))
              .with(Employee.HIREDATE, LocalDate.now())
              .with(Employee.COMMISSION, random.nextDouble() * 500)
              .build());
    }
  }
}
