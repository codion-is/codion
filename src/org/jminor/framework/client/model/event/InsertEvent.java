/*
 * Copyright (c) 2008, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.client.model.event;

import org.jminor.framework.model.EntityKey;

import java.awt.event.ActionEvent;
import java.util.List;

public class InsertEvent extends ActionEvent {

  private final List<EntityKey> insertedKeys;

  public InsertEvent(final Object source, final List<EntityKey> insertedKeys) {
    super(source, 0, "insert");
    this.insertedKeys = insertedKeys;
  }

  public List<EntityKey> getInsertedKeys() {
    return insertedKeys;
  }
}
