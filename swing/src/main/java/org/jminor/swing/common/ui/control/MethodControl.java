/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.swing.common.ui.control;

import org.jminor.common.Event;
import org.jminor.common.EventListener;
import org.jminor.common.Events;
import org.jminor.common.StateObserver;
import org.jminor.common.model.CancelException;
import org.jminor.common.model.Util;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A Control class for binding an action to a parameterless method via reflection.
 */
public final class MethodControl extends Control {

  private final Event actionPerformedEvent = Events.event();

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
   * @throws IllegalArgumentException if the method was not found in the owner object
   */
  public MethodControl(final String name, final Object owner, final String methodName, final StateObserver enabledState) {
    super(name, enabledState);
    Util.rejectNullValue(owner, "owner");
    Util.rejectNullValue(methodName, "methodName");
    this.owner = owner;
    try {
      this.method = owner.getClass().getMethod(methodName);
    }
    catch (final NoSuchMethodException e) {
      throw new IllegalArgumentException("Method " + methodName + " not found in class " + owner.getClass().getName(), e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void actionPerformed(final ActionEvent e) {
    try {
      method.invoke(owner);
    }
    catch (final InvocationTargetException ite) {
      final Throwable targetException = ite.getTargetException();
      if (!(targetException instanceof CancelException)) {
        if (targetException instanceof RuntimeException) {
          throw (RuntimeException) targetException;
        }
        else {
          throw new RuntimeException(targetException);
        }
      }
    }
    catch (final Exception ex) {
      throw new RuntimeException(ex);
    }
    finally {
      actionPerformedEvent.fire();
    }
  }

  /**
   * @param listener a listener notified each time actionPerformed() is called
   */
  public void addActionPerformedListener(final EventListener listener) {
    actionPerformedEvent.addListener(listener);
  }

  /**
   * @param listener the listener to remove
   */
  public void removeActionPerformedListener(final EventListener listener) {
    actionPerformedEvent.removeListener(listener);
  }
}