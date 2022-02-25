/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.model;

import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.domain.Chinook.Album;
import is.codion.framework.domain.entity.Attribute;
import is.codion.framework.domain.entity.Entity;
import is.codion.swing.framework.model.SwingEntityTableModel;

import java.text.NumberFormat;

public final class AlbumTableModel extends SwingEntityTableModel {

  private final NumberFormat kbFormat = NumberFormat.getIntegerInstance();

  public AlbumTableModel(EntityConnectionProvider connectionProvider) {
    super(Album.TYPE, connectionProvider);
  }

  @Override
  protected Object getValue(Entity entity, Attribute<?> attribute) {
    Object value = super.getValue(entity, attribute);
    if (value != null && attribute.equals(Album.COVER)) {
      byte[] bytes = (byte[]) value;

      return kbFormat.format(bytes.length / 1024) + " Kb";
    }

    return value;
  }
}
