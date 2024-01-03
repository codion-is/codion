/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.employees.testing.scenarios;

import is.codion.framework.demos.employees.model.EmployeesAppModel;
import is.codion.swing.framework.model.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Random;

// tag::loadTest[]
public final class LoginLogout extends AbstractEntityUsageScenario<EmployeesAppModel> {

  final Random random = new Random();

  @Override
  protected void perform(EmployeesAppModel application) {
    try {
      application.connectionProvider().close();
      Thread.sleep(random.nextInt(1500));
      application.connectionProvider().connection();
    }
    catch (InterruptedException ignored) {/*ignored*/}
  }

  @Override
  public int defaultWeight() {
    return 4;
  }
}
// end::loadTest[]