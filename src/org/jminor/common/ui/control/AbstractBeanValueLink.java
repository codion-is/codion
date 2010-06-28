/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import java.lang.reflect.Method;

/**
 * A bean based implementation of the AbstractValueLink class.
 */
public abstract class AbstractBeanValueLink extends AbstractValueLink<Object, Object> {

  private final String propertyName;
  private final Class<?> propertyClass;
  private final Method getMethod;
  private final Method setMethod;

  public AbstractBeanValueLink(final Object owner, final String propertyName, final Class<?> propertyClass,
                               final Event valueChangeEvent) {
    this(owner, propertyName, propertyClass, valueChangeEvent, LinkType.READ_WRITE);
  }

  public AbstractBeanValueLink(final Object owner, final String propertyName, final Class<?> propertyClass,
                               final Event valueChangeEvent, final LinkType linkType) {
    super(owner, valueChangeEvent, linkType);
    try {
      this.propertyName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      this.propertyClass = propertyClass;
      this.setMethod = linkType != LinkType.READ_ONLY ? getSetMethod() : null;
      this.getMethod = getGetMethod();
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException("Bean property methods for " + propertyName + " not found in class " + owner.getClass().getName(), e);
    }
  }

  /**
   * @return the name of the linked property
   */
  public String getPropertyName() {
    return propertyName;
  }

  @Override
  public Object getModelValue() {
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

  @Override
  public void setModelValue(final Object value) {
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
  protected Method getSetMethod() throws NoSuchMethodException {
    return getValueOwner().getClass().getMethod("set" + propertyName, propertyClass);
  }

  /**
   * @return the method used to get the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  protected Method getGetMethod() throws NoSuchMethodException {
    return getValueOwner().getClass().getMethod("get" + propertyName);
  }
}
