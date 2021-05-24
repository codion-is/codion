/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.IntegerField;

class DefaultIntegerFieldBuilder extends DefaultTextFieldBuilder<Integer, IntegerField, IntegerFieldBuilder> implements IntegerFieldBuilder {

  DefaultIntegerFieldBuilder() {
    super(Integer.class);
  }
}
