/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.manual.common;

import org.junit.jupiter.api.Test;

public final class InputControlsTest {

  @Test
  void test() {
    InputControls.basics();
    InputControls.checkBox();
    InputControls.nullableCheckBox();
    InputControls.booleanComboBox();
    InputControls.textField();
    InputControls.textArea();
    InputControls.integerField();
    InputControls.longField();
    InputControls.doubleField();
    InputControls.bigDecimalField();
    InputControls.localTime();
    InputControls.localDate();
    InputControls.localDateTime();
    InputControls.selectionComboBox();
  }
}
