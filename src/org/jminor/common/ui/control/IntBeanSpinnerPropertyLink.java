/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * User: Björn Darri
 * Date: 14.12.2007
 * Time: 23:58:50
 */
public class IntBeanSpinnerPropertyLink extends BeanPropertyLink {

  private final SpinnerNumberModel spinnerModel;

  public IntBeanSpinnerPropertyLink(final Object owner, final String propertyName, final Event propertyChangeEvent,
                                    final String text) {
    this(owner, propertyName, propertyChangeEvent, text, LinkType.READ_WRITE);
  }

  public IntBeanSpinnerPropertyLink(final Object owner, final String propertyName, final Event propertyChangeEvent,
                                    final String text, final LinkType linkType) {
    super(owner, propertyName, int.class, propertyChangeEvent, text, linkType);
    spinnerModel = new SpinnerNumberModel();
    spinnerModel.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent e) {
        updateModel();
      }
    });
    updateUI();
  }

  public SpinnerNumberModel getSpinnerModel() {
    return spinnerModel;
  }

  @Override
  protected Object getUIPropertyValue() {
    return spinnerModel.getValue();
  }

  @Override
  protected void setUIPropertyValue(final Object propertyValue) {
    spinnerModel.setValue(propertyValue);
  }
}
