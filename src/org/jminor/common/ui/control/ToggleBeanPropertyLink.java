/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.AbstractButton;
import java.lang.reflect.Method;

public class ToggleBeanPropertyLink extends BeanPropertyLink {

  private AbstractButton button;

  public ToggleBeanPropertyLink(final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String text) {
    this(owner, propertyName, propertyChangeEvent, text, LinkType.READ_WRITE);
  }

  public ToggleBeanPropertyLink(final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String text, final LinkType linkType) {
    super(owner, propertyName, boolean.class, propertyChangeEvent, text, linkType);
  }

  public void setButton(final AbstractButton toggleButton) {
    this.button = toggleButton;
    this.button.setAction(this);
    updateUI();
  }

  /** {@inheritDoc} */
  protected Method getGetMethod() throws NoSuchMethodException {
    try {
      return getPropertyOwner().getClass().getMethod("is" + getPropertyName());
    }
    catch (NoSuchMethodException e) {
      return super.getGetMethod();
    }
  }

  /** {@inheritDoc} */
  protected Object getUIPropertyValue() {
    return button.isSelected();
  }

  /** {@inheritDoc} */
  protected void setUIPropertyValue(final Object propertyValue) {
    final Boolean value = (Boolean) propertyValue;
    button.setSelected(value != null && value);
  }
}
