/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

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
    this(owner, propertyName, propertyChangeEvent, text, LinkType.READ_WRITE, null);
  }

  public IntBeanSpinnerPropertyLink(final Object owner, final String propertyName, final Event propertyChangeEvent,
                                    final String text, final LinkType linkType, final State enabledState) {
    super(owner, propertyName, int.class, propertyChangeEvent, text, linkType, enabledState);
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

  protected Object getUIPropertyValue() {
    return spinnerModel.getValue();
  }

  protected void setUIPropertyValue(final Object propertyValue) {
    spinnerModel.setValue(propertyValue);
  }
}
