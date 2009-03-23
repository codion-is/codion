/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.State;
import org.jminor.common.model.Util;
import org.jminor.common.ui.control.LinkType;
import org.jminor.common.ui.textfield.IntField;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Property;

public class IntTextPropertyLink extends TextPropertyLink {

  public IntTextPropertyLink(final EntityModel entityModel, final Property property, final IntField textField,
                             final boolean immediateUpdate, final LinkType linkType,
                             final State enableState) {
    super(entityModel, property, textField, immediateUpdate, linkType, null, enableState);
  }

  /** {@inheritDoc} */
  protected Object valueFromText(final String text) {
    try {
      return Util.getInt(text);
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
