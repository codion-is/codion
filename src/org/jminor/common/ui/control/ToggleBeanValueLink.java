/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;

import javax.swing.ButtonModel;
import javax.swing.JToggleButton;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

/**
 * Binds a ButtonModel to a boolean based bean property.
 */
public final class ToggleBeanValueLink extends AbstractBeanValueLink {

  private final ButtonModel buttonModel;

  /**
   * Instantiates a new ToggleBeanValueLink.
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public ToggleBeanValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent) {
    this(owner, propertyName, valueChangeEvent, null);
  }

  /**
   * Instantiates a new ToggleBeanValueLink.
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   */
  public ToggleBeanValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent,
                             final String caption) {
    this(owner, propertyName, valueChangeEvent, caption, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new ToggleBeanValueLink.
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   * @param linkType the link type
   */
  public ToggleBeanValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent,
                             final String caption, final LinkType linkType) {
    this(new JToggleButton.ToggleButtonModel(), owner, propertyName, valueChangeEvent, caption, linkType);
  }

  /**
   * Instantiates a new ToggleBeanValueLink.
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public ToggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                             final EventObserver valueChangeEvent) {
    this(buttonModel, owner, propertyName, valueChangeEvent, null);
  }

  /**
   * Instantiates a new ToggleBeanValueLink.
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   */
  public ToggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                             final EventObserver valueChangeEvent, final String caption) {
    this(buttonModel, owner, propertyName, valueChangeEvent, caption, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new ToggleBeanValueLink.
   * @param buttonModel the button model to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param caption the check box caption, if any
   * @param linkType the link type
   */
  public ToggleBeanValueLink(final ButtonModel buttonModel, final Object owner, final String propertyName,
                             final EventObserver valueChangeEvent, final String caption, final LinkType linkType) {
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

  /**
   * @return the button model
   */
  public ButtonModel getButtonModel() {
    return buttonModel;
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
