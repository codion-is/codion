/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.swing.model.EntityEditModel;
import org.jminor.framework.swing.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.ALBUM_ARTISTID_FK;
import static org.jminor.framework.demos.chinook.domain.Chinook.ALBUM_TITLE;

public class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(ALBUM_ARTISTID_FK);
    final JTextField txtArtist = createEntityLookupField(ALBUM_ARTISTID_FK);
    txtArtist.setColumns(18);
    final JTextField txtTitle = createTextField(ALBUM_TITLE);
    txtTitle.setColumns(18);

    setLayout(new GridLayout(2, 1, 5, 5));
    addPropertyPanel(ALBUM_ARTISTID_FK);
    addPropertyPanel(ALBUM_TITLE);
  }
}
