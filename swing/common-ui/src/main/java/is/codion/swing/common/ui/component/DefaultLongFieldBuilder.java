/*
 * Copyright (c) 2004 - 2021, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.swing.common.ui.component;

import is.codion.swing.common.ui.textfield.LongField;

class DefaultLongFieldBuilder extends DefaultTextFieldBuilder<Long, LongField, LongFieldBuilder> implements LongFieldBuilder {

  DefaultLongFieldBuilder() {
    super(Long.class);
  }
}
