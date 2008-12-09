/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

import javax.swing.AbstractButton;
import java.lang.reflect.Method;

public class ToggleBeanPropertyLink extends BeanPropertyLink {

  private AbstractButton button;

  public ToggleBeanPropertyLink(final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String text) {
    this(owner, propertyName, propertyChangeEvent, text, LinkType.READ_WRITE, null);
  }

  public ToggleBeanPropertyLink(final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String text, final LinkType linkType,
                                final State enabledState) {
    super(owner, propertyName, boolean.class, propertyChangeEvent, text, linkType, enabledState);
  }

  public void setButton(final AbstractButton toggleButton) {
    this.button = toggleButton;
    this.button.setAction(this);
    refreshUI();
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
  protected void updateProperty() {
    setPropertyValue(button.isSelected());
  }

  /** {@inheritDoc} */
  protected void updateUI() {
    final Boolean value = (Boolean) getPropertyValue();
    button.setSelected(value != null && value);
  }
}
