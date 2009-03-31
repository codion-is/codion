/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.model.Entity;

import java.awt.event.ActionEvent;
import java.util.List;

public class DeleteEvent extends ActionEvent {

  private final List<Entity> deletedEntities;

  public DeleteEvent(final Object source, final List<Entity> deletedEntities) {
    super(source, 0, "delete");
    this.deletedEntities = deletedEntities;
  }

  public List<Entity> getDeletedEntities() {
    return deletedEntities;
  }
}