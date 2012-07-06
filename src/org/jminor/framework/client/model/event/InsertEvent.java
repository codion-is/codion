/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.domain.Entity;

import java.util.List;

/**
 * An event describing a insert action.
 */
public final class InsertEvent {

  private final List<Entity> insertedEntities;

  /**
   * Instantiates a new InsertEvent.
   * @param insertedEntities the inserted entities
   */
  public InsertEvent(final List<Entity> insertedEntities) {
    this.insertedEntities = insertedEntities;
  }

  /**
   * @return the entities just inserted
   */
  public List<Entity> getInsertedEntities() {
    return insertedEntities;
  }
}
