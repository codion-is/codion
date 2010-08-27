/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.lang.ref.WeakReference;

/**
 * A PropertyChangeListener which does not prevent garbage collection.
 * @see WeakReference
 */
public final class WeakPropertyChangeListener implements PropertyChangeListener {

  private final WeakReference<PropertyChangeListener> listenerReference;

  /**
   * Instantiates a new WeakPropertyChangeListener
   * @param listener the PropertyChangeListener instance
   */
  public WeakPropertyChangeListener(final PropertyChangeListener listener) {
    listenerReference = new WeakReference<PropertyChangeListener>(listener);
  }

  /** {@inheritDoc} */
  public void propertyChange(final PropertyChangeEvent evt) {
    final PropertyChangeListener listener = listenerReference.get();
    if (listener == null) {
      removeListener(evt.getSource());
    }
    else {
      listener.propertyChange(evt);
    }
  }

  private void removeListener(final Object source) {
    try {
      source.getClass().getMethod("removePropertyChangeListener", PropertyChangeListener.class).invoke(source, this);
    }
    catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}
