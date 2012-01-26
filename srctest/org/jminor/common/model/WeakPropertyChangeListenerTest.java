/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.common.model;

import org.junit.Test;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class WeakPropertyChangeListenerTest {

  private int changeCounter = 0;
  private boolean removed = false;

  @Test
  public void test() {
    PropertyChangeListener listener = new PropertyChangeListener() {
      @Override
      public void propertyChange(final PropertyChangeEvent evt) {
        changeCounter++;
      }
    };
    final WeakPropertyChangeListener weakListener = new WeakPropertyChangeListener(listener);
    weakListener.propertyChange(new PropertyChangeEvent(this, "property", "old", "new"));
    assertEquals(1, changeCounter);
    //noinspection UnusedAssignment
    listener = null;
    System.gc();
    weakListener.propertyChange(new PropertyChangeEvent(this, "property", "old", "new"));
    assertTrue(removed);
    assertEquals(1, changeCounter);
  }

  @SuppressWarnings({"UnusedDeclaration"})
  public void removePropertyChangeListener(final PropertyChangeListener listener) {
    removed = true;
  }
}
