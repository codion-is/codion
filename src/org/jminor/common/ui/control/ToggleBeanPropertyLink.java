/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.lang.reflect.Method;

/**
 * Binds a ButtonModel to a boolean based bean property.
 */
public class ToggleBeanPropertyLink extends BeanPropertyLink {

  private final ButtonModel buttonModel;

  public ToggleBeanPropertyLink(final Object owner, final String propertyName, final Event propertyChangeEvent,
                                final String caption) {
    this(owner, propertyName, propertyChangeEvent, caption, LinkType.READ_WRITE);
  }

  public ToggleBeanPropertyLink(final Object owner, final String propertyName, final Event propertyChangeEvent,
                                final String caption, final LinkType linkType) {
    this(new JToggleButton.ToggleButtonModel(), owner, propertyName, propertyChangeEvent, caption, linkType);
  }

  public ToggleBeanPropertyLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String caption) {
    this(buttonModel, owner, propertyName, propertyChangeEvent, caption, LinkType.READ_WRITE);
  }

  public ToggleBeanPropertyLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                                final Event propertyChangeEvent, final String caption, final LinkType linkType) {
    super(owner, propertyName, boolean.class, propertyChangeEvent, linkType);
    this.buttonModel = buttonModel;
    this.buttonModel.addItemListener(new ItemListener() {
      public void itemStateChanged(final ItemEvent e) {
        updateModel();
      }
    });
    setName(caption);
    updateUI();
  }

  public ButtonModel getButtonModel() {
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
