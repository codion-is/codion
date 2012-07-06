/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.common.model.EventAdapter;

/**
 * A listener for update events.
 */
public abstract class UpdateListener extends EventAdapter<UpdateEvent> {

  /** {@inheritDoc} */
  @Override
  public final void eventOccurred(final UpdateEvent eventInfo) {
    updated(eventInfo);
  }

  /**
   * Handles the given update event
   * @param event the event to handle
   */
  protected abstract void updated(final UpdateEvent event);
}