/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.DoubleField;

class DefaultDoubleFieldBuilder extends DefaultTextFieldBuilder<Double, DoubleField, DoubleFieldBuilder> implements DoubleFieldBuilder {

  DefaultDoubleFieldBuilder() {
    super(Double.class);
  }
}
