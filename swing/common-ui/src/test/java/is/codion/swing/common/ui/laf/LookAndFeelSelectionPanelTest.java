/*
 * Copyright (c) 2010 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.laf;

import org.junit.jupiter.api.Test;

public final class LookAndFeelSelectionPanelTest {

  @Test
  void test() {
    new LookAndFeelSelectionPanel(true).getSelectedLookAndFeel();
  }
}
