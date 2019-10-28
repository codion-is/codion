/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.ALBUM_ARTIST_FK;
import static org.jminor.framework.demos.chinook.domain.Chinook.ALBUM_TITLE;

public class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(ALBUM_ARTIST_FK);

    createForeignKeyLookupField(ALBUM_ARTIST_FK).setColumns(18);
    createTextField(ALBUM_TITLE).setColumns(18);

    setLayout(new GridLayout(2, 1, 5, 5));
    addPropertyPanel(ALBUM_ARTIST_FK);
    addPropertyPanel(ALBUM_TITLE);
  }
}
