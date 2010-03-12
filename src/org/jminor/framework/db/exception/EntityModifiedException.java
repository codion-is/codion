/*
 * Copyright (c) 2009, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.db.exception;

import org.jminor.framework.domain.Entity;
import org.jminor.framework.i18n.FrameworkMessages;

/**
 * User: Björn Darri
 * Date: 26.9.2009
 * Time: 15:35:18
 */
public class EntityModifiedException extends Exception {

  private final Entity entity;
  private final Entity modifiedEntity;

  public EntityModifiedException(final Entity entity, final Entity modifiedEntity) {
    super(FrameworkMessages.get(FrameworkMessages.ENTITY_MODIFIED_EXCEPTION));
    this.entity = entity;
    this.modifiedEntity = modifiedEntity;
  }

  public Entity getEntity() {
    return entity;
  }

  public Entity getModifiedEntity() {
    return modifiedEntity;
  }
}
