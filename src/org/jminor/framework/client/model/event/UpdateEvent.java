/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.domain.Entity;

import java.util.List;

/**
 * An event describing a update action.
 */
public final class UpdateEvent {

  private final List<Entity> updatedEntities;
  private final boolean primaryKeyModified;

  /**
   * Instantiates a new UpdateEvent.
   * @param updatedEntities the updated entities
   * @param primaryKeyModified true if primary key values were modified during the update
   */
  public UpdateEvent(final List<Entity> updatedEntities, final boolean primaryKeyModified) {
    this.updatedEntities = updatedEntities;
    this.primaryKeyModified = primaryKeyModified;
  }

  /**
   * @return the updated entities
   */
  public List<Entity> getUpdatedEntities() {
    return updatedEntities;
  }

  /**
   * @return true if primary key values were modified during the update
   */
  public boolean isPrimaryKeyModified() {
    return primaryKeyModified;
  }
}