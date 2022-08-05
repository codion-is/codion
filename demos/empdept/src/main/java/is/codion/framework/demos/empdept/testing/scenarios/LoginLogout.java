/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
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
      application.getConnectionProvider().close();
      Thread.sleep(random.nextInt(1500));
      application.getConnectionProvider().connection();
    }
    catch (InterruptedException ignored) {/*ignored*/}
  }

  @Override
  public int getDefaultWeight() {
    return 4;
  }
}
// end::loadTest[]