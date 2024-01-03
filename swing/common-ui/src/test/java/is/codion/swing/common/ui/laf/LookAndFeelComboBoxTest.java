/*
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.laf;

import org.junit.jupiter.api.Test;

public final class LookAndFeelComboBoxTest {

  @Test
  void test() {
    LookAndFeelComboBox.lookAndFeelComboBox(true).selectedLookAndFeel();
  }
}
