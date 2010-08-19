/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.domain.Entity;

import java.awt.event.ActionEvent;
import java.util.List;

/**
 * An event describing a insert action.
 */
public final class InsertEvent extends ActionEvent {

  private final List<Entity.Key> insertedKeys;

  /**
   * Instantiates a new InsertEvent.
   * @param source the source of the insert
   * @param insertedKeys the primary keys of the inserted entities
   */
  public InsertEvent(final Object source, final List<Entity.Key> insertedKeys) {
    super(source, 0, "insert");
    this.insertedKeys = insertedKeys;
  }

  /**
   * @return the primary keys of the inserted entities
   */
  public List<Entity.Key> getInsertedKeys() {
    return insertedKeys;
  }
}
