/*
 * Copyright (c) 2020 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component.combobox;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class CompletionTest {

  @Test
  void setTwice() {
    JComboBox<?> comboBox = Completion.maximumMatch(new JComboBox<>());
    assertThrows(IllegalStateException.class, () -> Completion.maximumMatch(comboBox));
  }
}
