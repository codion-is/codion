/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.JToggleButton;
import java.lang.reflect.Method;

public class ToggleBeanPropertyLink extends BeanPropertyLink {

  private final JToggleButton.ToggleButtonModel buttonModel = new JToggleButton.ToggleButtonModel();

  public ToggleBeanPropertyLink(final Object owner, final String propertyName, final Event propertyChangeEvent,
                                final String caption) {
    this(owner, propertyName, propertyChangeEvent, caption, LinkType.READ_WRITE);
  }

  public ToggleBeanPropertyLink(final Object owner, final String propertyName, final Event propertyChangeEvent,
                                final String caption, final LinkType linkType) {
    super(owner, propertyName, boolean.class, propertyChangeEvent, linkType);
    this.buttonModel.addActionListener(this);
    setName(caption);
    updateUI();
  }

  public JToggleButton.ToggleButtonModel getButtonModel() {
    return buttonModel;
  }

  /** {@inheritDoc} */
  @Override
  protected Method getGetMethod() throws NoSuchMethodException {
    try {
      return getPropertyOwner().getClass().getMethod("is" + getPropertyName());
    }
    catch (NoSuchMethodException e) {
      return super.getGetMethod();
    }
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIPropertyValue() {
    return buttonModel.isSelected();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    final Boolean value = (Boolean) propertyValue;
    buttonModel.setSelected(value != null && value);
  }
}
