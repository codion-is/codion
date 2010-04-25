/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.valuemap;

import org.jminor.common.model.Util;
import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.IntField;

/**
 * A class for linking a IntField to a ChangeValueMapEditModel int key value.
 */
public class IntValueLink extends TextValueLink {

  public IntValueLink(final IntField textField, final ChangeValueMapEditModel<String, Object> editModel,
                      final String key, final boolean immediateUpdate, final LinkType linkType) {
    super(textField, editModel, key, immediateUpdate, linkType);
  }

  /** {@inheritDoc} */
  @Override
  protected Object valueFromText(final String text) {
    try {
      return Util.getInt(text);
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
