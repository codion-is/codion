/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.BigDecimalField;

import java.math.BigDecimal;

class DefaultBigDecimalFieldBuilder extends DefaultTextFieldBuilder<BigDecimal, BigDecimalField, BigDecimalFieldBuilder> implements BigDecimalFieldBuilder {

  DefaultBigDecimalFieldBuilder() {
    super(BigDecimal.class);
  }
}
