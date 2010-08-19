/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.domain.Entity;

import java.awt.event.ActionEvent;
import java.util.List;

/**
 * An event describing a update action.
 */
public final class UpdateEvent extends ActionEvent {

  private final List<Entity> updatedEntities;
  private final boolean primaryKeyModified;

  /**
   * Instantiates a new UpdateEvent.
   * @param source the source of the update
   * @param updatedEntities the udpated entities
   * @param primaryKeyModified true if primary key values were modified during the update
   */
  public UpdateEvent(final Object source, final List<Entity> updatedEntities, final boolean primaryKeyModified) {
    super(source, 0, "update");
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