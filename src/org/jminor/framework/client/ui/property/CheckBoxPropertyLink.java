/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.model.Property;
import org.jminor.framework.model.Type;

import javax.swing.ButtonModel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

public class CheckBoxPropertyLink extends AbstractEntityPropertyLink {

  private final ButtonModel buttonModel;

  public CheckBoxPropertyLink(final EntityModel entityModel, final Property property, final ButtonModel buttonModel) {
    this(entityModel, property, buttonModel, LinkType.READ_WRITE);
  }

  public CheckBoxPropertyLink(final EntityModel entityModel, final Property property, final ButtonModel buttonModel,
                              final LinkType linkType) {
    super(entityModel, property, linkType);
    this.buttonModel = buttonModel;
    updateUI();
    this.buttonModel.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        updateModel();
      }
    });
  }

  /** {@inheritDoc} */
  protected Object getUIPropertyValue() {
    return buttonModel.isSelected() ? Type.Boolean.TRUE : Type.Boolean.FALSE;
  }

  /** {@inheritDoc} */
  protected void setUIPropertyValue(final Object propertyValue) {
    buttonModel.setSelected(propertyValue == Type.Boolean.TRUE);
  }
}
