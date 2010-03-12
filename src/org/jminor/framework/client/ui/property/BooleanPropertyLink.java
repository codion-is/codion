/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Property;

import javax.swing.ButtonModel;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * A class for linking a UI component to a boolean value
 */
public class BooleanPropertyLink extends AbstractEntityPropertyLink {

  private final ButtonModel buttonModel;

  public BooleanPropertyLink(final ButtonModel buttonModel, final EntityEditModel editModel, final Property property) {
    this(buttonModel, editModel, property, LinkType.READ_WRITE);
  }

  public BooleanPropertyLink(final ButtonModel buttonModel, final EntityEditModel editModel, final Property property, final LinkType linkType) {
    super(editModel, property, linkType);
    this.buttonModel = buttonModel;
    this.buttonModel.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        updateModel();
      }
    });
    updateUI();
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIPropertyValue() {
    return buttonModel.isSelected();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    buttonModel.setSelected(propertyValue != null && (Boolean) propertyValue);
  }
}
