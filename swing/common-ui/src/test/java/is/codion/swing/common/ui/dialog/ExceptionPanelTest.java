/*
 * Copyright (c) 2010 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.dialog;

import org.junit.jupiter.api.Test;

public final class ExceptionPanelTest {

  @Test
  void test() {
    new ExceptionPanel(new Exception("Exception"), "Title");
  }
}
