/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.domain.Entity;

import java.util.List;

/**
 * An event describing a delete action.
 */
public final class DeleteEvent {

  private final List<Entity> deletedEntities;

  /**
   * Instantiates a new DeleteEvent.
   * @param deletedEntities the deleted entities
   */
  public DeleteEvent(final List<Entity> deletedEntities) {
    this.deletedEntities = deletedEntities;
  }

  /**
   * @return the deleted entities
   */
  public List<Entity> getDeletedEntities() {
    return deletedEntities;
  }
}