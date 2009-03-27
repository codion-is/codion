/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.Util;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.DoubleField;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Property;

/**
 * A class for linking a DoubleField to a EntityModel double property value
 */
public class DoubleTextPropertyLink extends TextPropertyLink {

  public DoubleTextPropertyLink(final EntityModel entityModel, final Property property, final DoubleField textField,
                                final boolean immediateUpdate, final LinkType linkType) {
    super(entityModel, property, textField, immediateUpdate, linkType, null);
  }

  /** {@inheritDoc} */
  protected Object valueFromText(final String text) {
    try {
      return Util.getDouble(text);
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
