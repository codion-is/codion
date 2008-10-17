package org.jminor.framework.client.model.event;

import org.jminor.framework.model.Entity;

import java.awt.event.ActionEvent;
import java.util.List;

public class UpdateEvent extends ActionEvent {

  private final List<Entity> updatedEntities;

  public UpdateEvent(final Object source, final List<Entity> updatedEntities) {
    super(source, 0, "update");
    this.updatedEntities = updatedEntities;
  }

  public List<Entity> getUpdatedEntities() {
    return updatedEntities;
  }
}