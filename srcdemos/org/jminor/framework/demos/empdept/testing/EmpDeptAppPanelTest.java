package org.jminor.framework.demos.empdept.testing;

import org.jminor.common.model.User;
import org.jminor.framework.client.ui.EntityApplicationPanelTestUnit;
import org.jminor.framework.demos.empdept.client.ui.EmpDeptAppPanel;

import org.junit.Test;

public class EmpDeptAppPanelTest extends EntityApplicationPanelTestUnit {

  public EmpDeptAppPanelTest() {
    super(EmpDeptAppPanel.class, User.UNIT_TEST_USER);
  }

  @Test
  public void initializePanel() throws Exception {
    testInitializePanel();
  }
}
