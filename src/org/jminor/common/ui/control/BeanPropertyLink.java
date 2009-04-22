/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;

import java.lang.reflect.Method;

public abstract class BeanPropertyLink extends AbstractPropertyLink {

  private final String propertyName;
  private final Class<?> propertyClass;
  private final Method getMethod;
  private final Method setMethod;

  public BeanPropertyLink(final Object owner, final String propertyName, final Class<?> propertyClass,
                          final Event propertyChangeEvent, final String name) {
    this(owner, propertyName, propertyClass, propertyChangeEvent, name, LinkType.READ_WRITE);
  }

  public BeanPropertyLink(final Object owner, final String propertyName, final Class<?> propertyClass,
                          final Event propertyChangeEvent, final String name, final LinkType linkType) {
    super(owner, name, propertyChangeEvent, linkType);
    try {
      this.propertyName = Character.toUpperCase(propertyName.charAt(0)) + propertyName.substring(1);
      this.propertyClass = propertyClass;
      this.setMethod = linkType != LinkType.READ_ONLY ? getSetMethod() : null;
      this.getMethod = getGetMethod();
    }
    catch (NoSuchMethodException e) {
      e.printStackTrace();
      System.out.println("Bean property methods for " + propertyName + " not found in class " + owner.getClass().getName());
      throw new RuntimeException(e.getMessage());
    }
  }

  /**
   * @return the name of the linked property
   */
  public String getPropertyName() {
    return propertyName;
  }

  /** {@inheritDoc} */
  public Object getModelPropertyValue() {
    try {
      return getMethod.invoke(getPropertyOwner());
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  public void setModelPropertyValue(final Object obj) {
    try {
      setMethod.invoke(getPropertyOwner(), obj);
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
    return getPropertyOwner().getClass().getMethod("set" + propertyName, propertyClass);
  }

  /**
   * @return the method used to get the value of the linked property
   * @throws NoSuchMethodException if the method does not exist in the owner class
   */
  protected Method getGetMethod() throws NoSuchMethodException {
    return getPropertyOwner().getClass().getMethod("get" + propertyName);
  }
}
