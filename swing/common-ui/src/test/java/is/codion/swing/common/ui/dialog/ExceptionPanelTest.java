/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import org.junit.jupiter.api.Test;

public final class ExceptionPanelTest {

  @Test
  void test() {
    new ExceptionPanel(new Exception("Exception"), "Title");
  }
}
