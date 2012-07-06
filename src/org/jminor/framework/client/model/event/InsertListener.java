/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.common.model.EventAdapter;

/**
 * A listener for insert events.
 */
public abstract class InsertListener extends EventAdapter<InsertEvent> {

  /** {@inheritDoc} */
  @Override
  public final void eventOccurred(final InsertEvent eventInfo) {
    inserted(eventInfo);
  }

  /**
   * Handles the given insert event
   * @param event the event to handle
   */
  protected abstract void inserted(final InsertEvent event);
}
