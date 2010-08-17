/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;

import java.lang.reflect.Method;

/**
 * A bean based implementation of the AbstractValueLink class.
 */
public abstract class AbstractBeanValueLink extends AbstractValueLink<Object, Object> {

  private final String propertyName;
  private final Class<?> valueClass;
  private final Method getMethod;
  private final Method setMethod;

  /**
   * Instantiates a new AbstractBeanValueLink.
   * @param owner the value owner
   * @param propertyName the name of the property
   * @param valueClass the value class
   * @param valueChangeEvent an event observer notified each time the value changes
   */
  public AbstractBeanValueLink(final Object owner, final String propertyName, final Class<?> valueClass,
                               final EventObserver valueChangeEvent) {
    this(owner, propertyName, valueClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  /**
   * Instantiates a new AbstractBeanValueLink.
   * @param owner the value owner
   * @param propertyName the name of the property
   * @param valueClass the value class
   * @param valueChangeEvent an event observer notified each time the value changes
   * @param linkType the link type
   */
  public AbstractBeanValueLink(final Object owner, final String propertyName, final Class<?> valueClass,
                               final EventObserver valueChangeEvent, final LinkType linkType) {
    super(owner, valueChangeEvent, linkType);
    try {
      this.propertyName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      this.valueClass = valueClass;
      this.getMethod = getGetMethod();
      if (linkType == LinkType.READ_ONLY) {
        this.setMethod = null;
      }
      else {
        this.setMethod = getSetMethod();
      }
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException("Bean property methods for " + propertyName + ", type: " + valueClass + " not found in class " + owner.getClass().getName(), e);
    }
  }

  /**
   * @return the name of the linked property
   */
  public final String getPropertyName() {
    return propertyName;
  }

  /** {@inheritDoc} */
  @Override
  public final Object getModelValue() {
    try {
      return getMethod.invoke(getValueOwner());
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public final void setModelValue(final Object value) {
    try {
      setMethod.invoke(getValueOwner(), value);
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /**
   * @return the method used to set the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  private Method getSetMethod() throws NoSuchMethodException {
    return getValueOwner().getClass().getMethod("set" + propertyName, valueClass);
  }

  /**
   * @return the method used to get the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  private Method getGetMethod() throws NoSuchMethodException {
    if (valueClass.equals(boolean.class) || valueClass.equals(Boolean.class)) {
      try {
        return getValueOwner().getClass().getMethod("is" + propertyName);
      }
      catch (NoSuchMethodException e) {/**/}
    }

    return getValueOwner().getClass().getMethod("get" + propertyName);
  }
}
