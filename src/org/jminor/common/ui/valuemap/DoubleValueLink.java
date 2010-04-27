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
public class DoubleValueLink extends TextValueLink {

  public DoubleValueLink(final DoubleField textField, final ValueChangeMapEditModel<String, Object> editModel,
                         final String key, final boolean immediateUpdate, final LinkType linkType) {
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
