/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.combobox;

import org.junit.jupiter.api.Test;

import javax.swing.JComboBox;

import static org.junit.jupiter.api.Assertions.assertThrows;

public final class CompletionDocumentTest {

  @Test
  public void setTwice() {
    final JComboBox<?> comboBox = MaximumMatch.enable(new JComboBox<>());
    assertThrows(IllegalStateException.class,() -> MaximumMatch.enable(comboBox));
  }
}
