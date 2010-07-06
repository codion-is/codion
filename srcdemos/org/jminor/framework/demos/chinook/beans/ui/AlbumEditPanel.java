/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.ALBUM_ARTISTID_FK;
import static org.jminor.framework.demos.chinook.domain.Chinook.ALBUM_TITLE;

import javax.swing.JTextField;
import java.awt.GridLayout;

public class AlbumEditPanel extends EntityEditPanel {

  public AlbumEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusComponentKey(ALBUM_ARTISTID_FK);
    final JTextField txtArtists = createEntityLookupField(ALBUM_ARTISTID_FK);
    txtArtists.setColumns(18);
    final JTextField txtTitle = createTextField(ALBUM_TITLE);
    txtTitle.setColumns(18);

    setLayout(new GridLayout(2, 1, 5, 5));
    add(createPropertyPanel(ALBUM_ARTISTID_FK, txtArtists));
    add(createPropertyPanel(ALBUM_TITLE, txtTitle));
  }
}
