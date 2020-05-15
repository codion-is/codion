/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package dev.codion.framework.demos.manual.common;

import org.junit.jupiter.api.Test;

public final class InputControlsTest {

  @Test
  void test() {
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
