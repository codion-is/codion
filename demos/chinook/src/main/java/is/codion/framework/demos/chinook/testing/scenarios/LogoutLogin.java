/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.common.model.tools.loadtest.AbstractUsageScenario;

import java.util.Random;

public final class LogoutLogin extends AbstractUsageScenario<EntityConnectionProvider> {

  private final Random random = new Random();

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) {
    try {
      connectionProvider.close();
      Thread.sleep(random.nextInt(1500));
      connectionProvider.connection();
    }
    catch (InterruptedException ignored) {/*ignored*/}
  }
}
