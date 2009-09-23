/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.Util;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Property;

/**
 * A class for linking a DoubleField to a EntityModel double property value
 */
public class DoubleTextPropertyLink extends TextPropertyLink {

  public DoubleTextPropertyLink(final DoubleField textField, final EntityEditModel editModel, final Property property,
                                final boolean immediateUpdate, final LinkType linkType) {
    super(textField, editModel, property, immediateUpdate, linkType, null);
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
