/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Util;
import org.jminor.common.ui.textfield.IntField;

/**
 * Binds a DoubleField to a int based bean property.
 */
public class IntBeanValueLink extends TextBeanValueLink {

  /**
   * Instantiates a new IntBeanValueLink.
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   */
  public IntBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                          final EventObserver valueChangeEvent) {
    this(intField, owner, propertyName, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new IntBeanValueLink.
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   */
  public IntBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                          final EventObserver valueChangeEvent, final LinkType linkType) {
    this(intField, owner, propertyName, valueChangeEvent, linkType, true);
  }



  /**
   * Instantiates a new IntBeanValueLink.
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   */
  public IntBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                          final EventObserver valueChangeEvent, final boolean usePrimitive) {
    this(intField, owner, propertyName, valueChangeEvent, LinkType.READ_WRITE, usePrimitive);
  }

  /**
   * Instantiates a new IntBeanValueLink.
   * @param intField the int field to link with the value
   * @param owner the value owner
   * @param propertyName the property name
   * @param valueChangeEvent an EventObserver notified each time the value changes
   * @param linkType the link type
   * @param usePrimitive if true then the property is assumed to be a primitive, int instead of Integer
   */
  public IntBeanValueLink(final IntField intField, final Object owner, final String propertyName,
                          final EventObserver valueChangeEvent, final LinkType linkType, final boolean usePrimitive) {
    super(intField, owner, propertyName, usePrimitive ? int.class : Integer.class, valueChangeEvent, linkType);
    updateUI();
  }

  /** {@inheritDoc} */
  @Override
  protected final Object getUIValue() {
    final String text = getText();
    if (text.isEmpty() && getValueClass().equals(int.class)) {
      return 0;
    }
    try {
      return Util.getInt(getText());
    }
    catch (NumberFormatException nf) {
      throw new RuntimeException(nf);
    }
  }
}
