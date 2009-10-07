/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.ui.control;

import org.jminor.common.model.Event;
import org.jminor.common.model.State;

import java.awt.event.ActionEvent;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * A Control class for binding an action to a parameterless method via reflection
 */
public class MethodControl extends Control {

  public final Event evtActionPerfomed = new Event();

  private final Object owner;
  private final Method method;

  public MethodControl(final String name, final Object owner, final String methodName, final State enabledState) {
    super(name, enabledState);
    this.owner = owner;
    try {
      this.method = owner.getClass().getMethod(methodName);
    }
    catch (NoSuchMethodException e) {
      System.out.println("Method " + methodName + " not found in class " + owner.getClass().getName());
      throw new RuntimeException(e);
    }
  }

  /** {@inheritDoc} */
  @Override
  public void actionPerformed(final ActionEvent event) {
    try {
      method.invoke(owner);
    }
    catch (InvocationTargetException ite) {
      throw new RuntimeException(ite.getTargetException());
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    finally {
      evtActionPerfomed.fire();
    }
  }
}