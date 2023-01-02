/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.common;

import is.codion.framework.demos.manual.common.demo.ApplicationModel;
import is.codion.framework.demos.manual.common.demo.ApplicationPanel;

import org.junit.jupiter.api.Test;

public final class ApplicationPanelTest {

  @Test
  void create() {
    new ApplicationPanel(new ApplicationModel());
  }
}
