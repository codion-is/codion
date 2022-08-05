/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Random;

public final class LogoutLogin extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  private final Random random = new Random();

  @Override
  protected void perform(ChinookApplicationModel application) throws Exception {
    try {
      application.getConnectionProvider().close();
      Thread.sleep(random.nextInt(1500));
      application.getConnectionProvider().connection();
    }
    catch (InterruptedException ignored) {/*ignored*/}
  }
}
