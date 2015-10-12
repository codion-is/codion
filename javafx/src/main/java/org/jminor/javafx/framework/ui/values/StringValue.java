/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.javafx.framework.ui.values;

import org.jminor.common.model.Value;

import javafx.util.StringConverter;

public interface StringValue<V> extends Value<V> {

  StringConverter<V> getConverter();
}
