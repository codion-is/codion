/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.common.model.EventAdapter;

/**
 * A listener for delete events.
 */
public abstract class DeleteListener extends EventAdapter<DeleteEvent> {

  /** {@inheritDoc} */
  @Override
  public final void eventOccurred(final DeleteEvent eventInfo) {
    deleted(eventInfo);
  }

  /**
   * Handles the given delete event
   * @param event the event to handle
   */
  protected abstract void deleted(final DeleteEvent event);
}