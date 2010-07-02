/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.*;

import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class AlbumPanel extends EntityPanel {

  public AlbumPanel(final EntityModel model) {
    super(model, new AlbumEditPanel(model.getEditModel()));
    addDetailPanel(new TrackPanel(model.getDetailModel(T_TRACK)));
  }

  static class AlbumEditPanel extends EntityEditPanel {

    AlbumEditPanel(final EntityEditModel editModel) {
      super(editModel);
    }

    @Override
    protected void initializeUI() {
      setLayout(new GridLayout(2, 1, 5, 5));
      final JTextField txtArtists = createEntityLookupField(ALBUM_ARTISTID_FK);
      txtArtists.setColumns(18);
      setInitialFocusComponent(txtArtists);
      final JTextField txtTitle = createTextField(ALBUM_TITLE);
      txtTitle.setColumns(18);
      add(createPropertyPanel(ALBUM_ARTISTID_FK, txtArtists));
      add(createPropertyPanel(ALBUM_TITLE, txtTitle));
    }
  }
}
