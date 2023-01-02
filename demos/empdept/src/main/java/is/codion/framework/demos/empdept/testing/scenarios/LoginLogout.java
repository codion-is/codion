/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.empdept.testing.scenarios;

import is.codion.framework.demos.empdept.ui.EmpDeptAppPanel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Random;

// tag::loadTest[]
public final class LoginLogout extends AbstractEntityUsageScenario<EmpDeptAppPanel.EmpDeptApplicationModel> {

  final Random random = new Random();

  @Override
  protected void perform(EmpDeptAppPanel.EmpDeptApplicationModel application) {
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