/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.domain.Entity;

import java.awt.event.ActionEvent;
import java.util.List;

/**
 * An event describing a delete action.
 */
public final class DeleteEvent extends ActionEvent {

  private final List<Entity> deletedEntities;

  /**
   * Instantiates a new DeleteEvent.
   * @param source the source of the delete
   * @param deletedEntities the deleted entities
   */
  public DeleteEvent(final Object source, final List<Entity> deletedEntities) {
    super(source, 0, "delete");
    this.deletedEntities = deletedEntities;
  }

  /**
   * @return the deleted entities
   */
  public List<Entity> getDeletedEntities() {
    return deletedEntities;
  }
}