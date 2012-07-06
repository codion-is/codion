/*
 * Copyright (c) 2004 - 2011, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.EventObserver;
import org.jminor.common.model.Events;

import org.junit.Test;

public class AbstractBeanValueLinkTest {

  @Test
  public void constructor() {
    new BeanValueLink(this, "a", String.class, Events.event(), LinkType.READ_WRITE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullOwner() {
    new BeanValueLink(null, "a", String.class, Events.event(), LinkType.READ_WRITE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullPropertyName() {
    new BeanValueLink(this, null, String.class, Events.event(), LinkType.READ_WRITE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorEmptyPropertyName() {
    new BeanValueLink(this, "", String.class, Events.event(), LinkType.READ_WRITE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullPropertyClass() {
    new BeanValueLink(this, "a", null, Events.event(), LinkType.READ_WRITE);
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructorNullLinkType() {
    new BeanValueLink(this, "a", String.class, Events.event(), null);
  }

  private String a;

  public String getA() {
    return a;
  }

  public void setA(final String a) {
    this.a = a;
  }

  private static final class BeanValueLink extends AbstractBeanValueLink {
    private BeanValueLink(final Object owner, final String propertyName, final Class<?> valueClass,
                          final EventObserver valueChangeEvent, final LinkType linkType) {
      super(owner, propertyName, valueClass, valueChangeEvent, linkType);
    }

    @Override
    protected Object getUIValue() {
      return null;
    }

    @Override
    protected void setUIValue(final Object value) {}
  }
}
