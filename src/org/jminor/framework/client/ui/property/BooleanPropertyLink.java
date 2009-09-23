/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.ui.property;

import org.jminor.common.ui.control.LinkType;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.domain.Property;
import org.jminor.framework.domain.Type;

import javax.swing.JToggleButton;

/**
 * A class for linking a UI component to a boolean value
 */
public class BooleanPropertyLink extends AbstractEntityPropertyLink {

  private final JToggleButton.ToggleButtonModel buttonModel = new JToggleButton.ToggleButtonModel();

  public BooleanPropertyLink(final EntityEditModel editModel, final Property property) {
    this(editModel, property, LinkType.READ_WRITE);
  }

  public BooleanPropertyLink(final EntityEditModel editModel, final Property property, final LinkType linkType) {
    super(editModel, property, linkType);
    this.buttonModel.addActionListener(this);
    updateUI();
  }

  public JToggleButton.ToggleButtonModel getButtonModel() {
    return buttonModel;
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIPropertyValue() {
    return buttonModel.isSelected() ? Type.Boolean.TRUE : Type.Boolean.FALSE;
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    buttonModel.setSelected(propertyValue == Type.Boolean.TRUE);
  }
}
