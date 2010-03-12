/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.domain.Entity;

import java.awt.event.ActionEvent;
import java.util.List;

public class InsertEvent extends ActionEvent {

  private final List<Entity.Key> insertedKeys;

  public InsertEvent(final Object source, final List<Entity.Key> insertedKeys) {
    super(source, 0, "insert");
    this.insertedKeys = insertedKeys;
  }

  public List<Entity.Key> getInsertedKeys() {
    return insertedKeys;
  }
}
