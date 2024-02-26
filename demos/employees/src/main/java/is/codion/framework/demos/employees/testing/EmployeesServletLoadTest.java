/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.employees.testing;

import is.codion.common.Text;
import is.codion.common.model.CancelException;
import is.codion.common.model.loadtest.LoadTest;
import is.codion.common.model.loadtest.LoadTest.Scenario;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.db.http.HttpEntityConnectionProvider;
import is.codion.framework.demos.employees.domain.Employees;
import is.codion.framework.demos.employees.domain.Employees.Department;
import is.codion.framework.demos.employees.domain.Employees.Employee;
import is.codion.framework.domain.entity.Entity;
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

  private final LoadTest<EntityConnectionProvider> loadTest;

  private EmployeesServletLoadTest(User user) {
    loadTest = LoadTest.builder(EmployeesServletLoadTest::createApplication, EmployeesServletLoadTest::disconnectApplication)
            .user(user)
            .scenarios(asList(
                    Scenario.builder(new SelectDepartment())
                            .defaultWeight(4)
                            .build(),
                    Scenario.builder(new UpdateLocation())
                            .defaultWeight(2)
                            .build(),
                    Scenario.builder(new SelectEmployees())
                            .defaultWeight(5)
                            .build(),
                    Scenario.builder(new AddDepartment())
                            .defaultWeight(1)
                            .build(),
                    Scenario.builder(new AddEmployee())
                            .defaultWeight(4)
                            .build()))
            .minimumThinkTime(2500)
            .maximumThinkTime(5000)
            .loginDelayFactor(2)
            .applicationBatchSize(10)
            .build();
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
    new LoadTestPanel<>(LoadTestModel.loadTestModel(new EmployeesServletLoadTest(UNIT_TEST_USER).loadTest)).run();
  }

  private static final class UpdateLocation implements Scenario.Performer<EntityConnectionProvider> {

    @Override
    public void perform(EntityConnectionProvider client) throws Exception {
      List<Entity> departments = client.connection().select(all(Department.TYPE));
      Entity entity = departments.get(new Random().nextInt(departments.size()));
      entity.put(Department.LOCATION, Text.randomString(10, 13));
      client.connection().update(entity);
    }
  }

  private static final class SelectDepartment implements Scenario.Performer<EntityConnectionProvider> {

    @Override
    public void perform(EntityConnectionProvider client) throws Exception {
      client.connection().select(Department.NAME.equalTo("Accounting"));
    }
  }

  private static final class SelectEmployees implements Scenario.Performer<EntityConnectionProvider> {

    @Override
    public void perform(EntityConnectionProvider client) throws Exception {
      List<Entity> departments = client.connection().select(all(Department.TYPE));

      client.connection().select(Employee.DEPARTMENT
              .equalTo(departments.get(new Random().nextInt(departments.size())).get(Department.DEPARTMENT_NO)));
    }
  }

  private static final class AddDepartment implements Scenario.Performer<EntityConnectionProvider> {

    @Override
    public void perform(EntityConnectionProvider client) throws Exception {
      int departmentNo = new Random().nextInt(5000);
      client.connection().insert(client.entities().builder(Department.TYPE)
              .with(Department.DEPARTMENT_NO, departmentNo)
              .with(Department.NAME, Text.randomString(4, 8))
              .with(Department.LOCATION, Text.randomString(5, 10))
              .build());
    }
  }

  private static final class AddEmployee implements Scenario.Performer<EntityConnectionProvider> {

    private final Random random = new Random();

    @Override
    public void perform(EntityConnectionProvider client) throws Exception {
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
