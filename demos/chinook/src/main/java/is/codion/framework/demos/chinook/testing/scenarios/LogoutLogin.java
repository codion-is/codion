/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.common.model.loadtest.AbstractUsageScenario;
import is.codion.framework.db.EntityConnectionProvider;

import static is.codion.framework.demos.chinook.testing.scenarios.LoadTestUtil.RANDOM;

public final class LogoutLogin extends AbstractUsageScenario<EntityConnectionProvider> {

  @Override
  protected void perform(EntityConnectionProvider connectionProvider) {
    try {
      connectionProvider.close();
      Thread.sleep(RANDOM.nextInt(1500));
      connectionProvider.connection();
    }
    catch (InterruptedException ignored) {/*ignored*/}
  }
}
