/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.domain.Entity;

import java.awt.event.ActionEvent;
import java.util.List;

public class UpdateEvent extends ActionEvent {

  private final List<Entity> updatedEntities;
  private final boolean primaryKeyModified;

  public UpdateEvent(final Object source, final List<Entity> updatedEntities, final boolean primaryKeyModified) {
    super(source, 0, "update");
    this.updatedEntities = updatedEntities;
    this.primaryKeyModified = primaryKeyModified;
  }

  public List<Entity> getUpdatedEntities() {
    return updatedEntities;
  }

  public boolean isPrimaryKeyModified() {
    return primaryKeyModified;
  }
}