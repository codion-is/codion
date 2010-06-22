/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Binds a SpinnerModel to a int based bean property.
 * User: Bjorn Darri<br>
 * Date: 14.12.2007<br>
 * Time: 23:58:50<br>
 */
public class IntBeanSpinnerValueLink extends AbstractBeanValueLink {

  private final SpinnerNumberModel spinnerModel;

  public IntBeanSpinnerValueLink(final Object owner, final String propertyName, final Event propertyChangeEvent) {
    this(owner, propertyName, propertyChangeEvent, LinkType.READ_WRITE);
  }

  public IntBeanSpinnerValueLink(final Object owner, final String propertyName, final Event propertyChangeEvent,
                                    final LinkType linkType) {
    super(owner, propertyName, int.class, propertyChangeEvent, linkType);
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
  protected Object getUIValue() {
    return spinnerModel.getValue();
  }

  @Override
  protected void setUIValue(final Object value) {
    spinnerModel.setValue(value);
  }
}
