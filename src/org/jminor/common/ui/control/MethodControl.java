/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.Events;
import org.jminor.common.model.StateObserver;
import org.jminor.common.model.Util;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A Control class for binding an action to a parameterless method via reflection.
 */
public final class MethodControl extends Control {

  private final Event evtActionPerformed = Events.event();

  private final Object owner;
  private final Method method;

  /**
   * Instantiates a new MethodControl object
   * @param name the name of this control, used when a caption is required
   * @param owner the object owning the method being called
   * @param methodName the name of the method to call
   * @throws RuntimeException if the method was not found in the owner object
   */
  public MethodControl(final String name, final Object owner, final String methodName) {
    this(name, owner, methodName, null);
  }

  /**
   * Instantiates a new MethodControl object
   * @param name the name of this control, used when a caption is required
   * @param owner the object owning the method being called
   * @param methodName the name of the method to call
   * @param enabledState if specified then this control will only be enabled when this state is
   * @throws RuntimeException if the method was not found in the owner object
   */
  public MethodControl(final String name, final Object owner, final String methodName, final StateObserver enabledState) {
    super(name, enabledState);
    Util.rejectNullValue(owner, "owner");
    Util.rejectNullValue(methodName, "methodName");
    this.owner = owner;
    try {
      this.method = owner.getClass().getMethod(methodName);
    }
    catch (NoSuchMethodException e) {
      throw new RuntimeException("Method " + methodName + " not found in class " + owner.getClass().getName(), e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void actionPerformed(final ActionEvent e) {
    try {
      method.invoke(owner);
    }
    catch (InvocationTargetException ite) {
      throw new RuntimeException(ite.getTargetException());
    }
    catch (RuntimeException re) {
      throw re;
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    finally {
      evtActionPerformed.fire();
    }
  }

  /**
   * @param listener a listener notified each time action performed is called
   */
  public void addActionPerformedListener(final ActionListener listener) {
    evtActionPerformed.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeActionPerformedListener(final ActionListener listener) {
    evtActionPerformed.removeListener(listener);
  }
}