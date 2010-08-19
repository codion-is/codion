/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;

import javax.swing.SpinnerNumberModel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

/**
 * Binds a SpinnerModel to a int based bean property.
 */
public final class IntBeanSpinnerValueLink extends AbstractBeanValueLink {

  private final SpinnerNumberModel spinnerModel;

  /**
   * Instantiates a new IntBeanSpinnerValueLink.
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public IntBeanSpinnerValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent) {
    this(owner, propertyName, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new IntBeanSpinnerValueLink.
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public IntBeanSpinnerValueLink(final Object owner, final String propertyName, final EventObserver valueChangeEvent,
                                 final LinkType linkType) {
    super(owner, propertyName, int.class, valueChangeEvent, linkType);
    spinnerModel = new SpinnerNumberModel();
    spinnerModel.addChangeListener(new ChangeListener() {
      /** {@inheritDoc} */
      public void stateChanged(final ChangeEvent e) {
        updateModel();
      }
    });
    updateUI();
  }

  /**
   * @return the spinner model
   */
  public SpinnerNumberModel getSpinnerModel() {
    return spinnerModel;
  }

  /** {@inheritDoc} */
  @Override
  protected Object getUIValue() {
    return spinnerModel.getValue();
  }

  /** {@inheritDoc} */
  @Override
  protected void setUIValue(final Object value) {
    spinnerModel.setValue(value);
  }
}
