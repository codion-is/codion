/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.model.State;
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
    this(entityModel, property, buttonModel, LinkType.READ_WRITE, null);
  }

  public CheckBoxPropertyLink(final EntityModel entityModel, final Property property, final ButtonModel buttonModel,
                              final LinkType linkType, final State enabledState) {
    super(entityModel, property, linkType, enabledState);
    this.buttonModel = buttonModel;
    refreshUI();
    this.buttonModel.addItemListener(new ItemListener() {
      public void itemStateChanged(ItemEvent e) {
        refreshProperty();
      }
    });
  }

  /** {@inheritDoc} */
  protected Object getUiPropertyValue() {
    return buttonModel.isSelected() ? Type.Boolean.TRUE : Type.Boolean.FALSE;
  }

  /** {@inheritDoc} */
  protected void setUiPropertyValue(final Object propertyValue) {
    buttonModel.setSelected(propertyValue == Type.Boolean.TRUE);
  }
}
