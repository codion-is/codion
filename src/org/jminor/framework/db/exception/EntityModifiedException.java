package org.jminor.framework.db.exception;

import org.jminor.framework.domain.Entity;

/**
 * User: Björn Darri
 * Date: 26.9.2009
 * Time: 15:35:18
 */
public class EntityModifiedException extends Exception {

  private final Entity modifiedEntity;

  public EntityModifiedException(final Entity entity) {
    this.modifiedEntity = entity;
  }

  public Entity getModifiedEntity() {
    return modifiedEntity;
  }
}
