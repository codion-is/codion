package is.codion.framework.demos.chinook.testing.scenarios;

import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.swing.common.tools.loadtest.ScenarioException;
import is.codion.swing.framework.tools.loadtest.EntityLoadTestModel;

import java.util.Random;

public final class LogoutLogin extends EntityLoadTestModel.AbstractEntityUsageScenario<ChinookApplicationModel> {

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
