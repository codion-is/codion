/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

import javax.swing.ButtonModel;
import javax.swing.DefaultButtonModel;
import java.awt.event.ActionEvent;
import java.lang.reflect.Method;

public class ToggleBeanPropertyLink extends BeanPropertyLink {

  private final ButtonModel buttonModel;

  public ToggleBeanPropertyLink(final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String text, final LinkType linkType,
                                final State enabledState) {
    super(owner, propertyName, boolean.class, propertyChangeEvent, text, linkType, enabledState);
    this.buttonModel = new DefaultButtonModel() {
      public boolean isSelected() {
        final Boolean value = (Boolean) getPropertyValue();
        return value != null && value;
      }
    };
    refreshUI();
  }

  /**
   * @return Value for property 'buttonModel'.
   */
  public ButtonModel getButtonModel() {
    return buttonModel;
  }

  /** {@inheritDoc} */
  public void actionPerformed(final ActionEvent e) {
    setPropertyValue(!(Boolean) getPropertyValue());
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
  protected void updateProperty() {}

  /** {@inheritDoc} */
  protected void updateUI() {
    final Boolean value = (Boolean) getPropertyValue();
    buttonModel.setSelected(value != null && value);
  }
}
