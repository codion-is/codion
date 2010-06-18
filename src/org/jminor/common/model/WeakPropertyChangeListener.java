/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

/**
 * A PropertyChangeListener which does not prevent garbage collection.<br>
 * @see WeakReference
 * User: Bjorn Darri<br>
 * Date: 12.4.2009<br>
 * Time: 16:34:25<br>
 */
public class WeakPropertyChangeListener implements PropertyChangeListener {

  private final WeakReference<PropertyChangeListener> listenerReference;

  public WeakPropertyChangeListener(final PropertyChangeListener listener) {
    listenerReference = new WeakReference<PropertyChangeListener>(listener);
  }

  public void propertyChange(final PropertyChangeEvent event) {
    final PropertyChangeListener listener = listenerReference.get();
    if (listener == null) {
      removeListener(event.getSource());
    }
    else {
      listener.propertyChange(event);
    }
  }

  private void removeListener(final Object source) {
    try {
      source.getClass().getMethod("removePropertyChangeListener", PropertyChangeListener.class).invoke(source, this);
    }
    catch (Exception e) {
      System.out.println("Could not remove listener: " + e);
    }
  }
}
