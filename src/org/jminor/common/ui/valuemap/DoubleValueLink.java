/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ValueChangeMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;

/**
 * A class for linking a DoubleField to a ValueChangeMapEditModel double property value.
 */
public class DoubleValueLink<T> extends TextValueLink<T> {

  public DoubleValueLink(final DoubleField textField, final ValueChangeMapEditModel<T, Object> editModel,
                         final T key, final boolean immediateUpdate, final LinkType linkType) {
    super(textField, editModel, key, immediateUpdate, linkType);
  }

  /** {@inheritDoc} */
  @Override
  protected Object valueFromText(final String text) {
    try {
      return Util.getDouble(text);
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
