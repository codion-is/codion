/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.Util;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.domain.Property;

/**
 * A class for linking a IntField to a EntityModel int property value
 */
public class IntTextPropertyLink extends TextPropertyLink {

  public IntTextPropertyLink(final IntField textField, final EntityModel entityModel, final Property property,
                             final boolean immediateUpdate, final LinkType linkType) {
    super(textField, entityModel, property, immediateUpdate, linkType, null);
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
