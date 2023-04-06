/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.swing.framework.tools.loadtest.AbstractEntityUsageScenario;

import java.util.Random;

public final class LogoutLogin extends AbstractEntityUsageScenario<ChinookAppModel> {

  private final Random random = new Random();

  @Override
  protected void perform(ChinookAppModel application) throws Exception {
    try {
      application.connectionProvider().close();
      Thread.sleep(random.nextInt(1500));
      application.connectionProvider().connection();
    }
    catch (InterruptedException ignored) {/*ignored*/}
  }
}
