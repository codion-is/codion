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
public class ToggleBeanValueLink extends AbstractBeanValueLink {

  private final ButtonModel buttonModel;

  public ToggleBeanValueLink(final Object owner, final String propertyName, final Event valueChangeEvent) {
    this(owner, propertyName, valueChangeEvent, null);
  }

  public ToggleBeanValueLink(final Object owner, final String propertyName, final Event valueChangeEvent,
                             final String caption) {
    this(owner, propertyName, valueChangeEvent, caption, LinkType.READ_WRITE);
  }

  public ToggleBeanValueLink(final Object owner, final String propertyName, final Event valueChangeEvent,
                             final String caption, final LinkType linkType) {
    this(new JToggleButton.ToggleButtonModel(), owner, propertyName, valueChangeEvent, caption, linkType);
  }

  public ToggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                             final Event valueChangeEvent) {
    this(buttonModel, owner, propertyName, valueChangeEvent, null);
  }

  public ToggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                             final Event valueChangeEvent, final String caption) {
    this(buttonModel, owner, propertyName, valueChangeEvent, caption, LinkType.READ_WRITE);
  }

  public ToggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                             final Event valueChangeEvent, final String caption, final LinkType linkType) {
    super(owner, propertyName, boolean.class, valueChangeEvent, linkType);
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
      return getValueOwner().getClass().getMethod("is" + getPropertyName());
    }
    catch (NoSuchMethodException e) {
      return super.getGetMethod();
    }
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    return buttonModel.isSelected();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    final Boolean booleanValue = (Boolean) value;
    buttonModel.setSelected(booleanValue != null && booleanValue);
  }
}
