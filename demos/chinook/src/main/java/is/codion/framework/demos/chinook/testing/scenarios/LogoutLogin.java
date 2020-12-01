/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Random;

public final class LogoutLogin extends AbstractEntityUsageScenario<ChinookApplicationModel> {

  private final Random random = new Random();

  @Override
  protected void perform(final ChinookApplicationModel application) throws ScenarioException {
    try {
      application.getConnectionProvider().close();
      Thread.sleep(random.nextInt(1500));
      application.getConnectionProvider().getConnection();
    }
    catch (final InterruptedException ignored) {/*ignored*/}
  }
}
