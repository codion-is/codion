/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.model.ChangeValueMapEditModel;
import org.jminor.common.ui.ChangeValueMapEditPanel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.chinook.beans.TrackModel;
import static org.jminor.framework.demos.chinook.domain.Chinook.ALBUM_ARTISTID_FK;
import static org.jminor.framework.demos.chinook.domain.Chinook.ALBUM_TITLE;

import javax.swing.JTextField;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class AlbumPanel extends EntityPanel {

  public AlbumPanel(final EntityModel model) {
    super(model, "Albums");
  }

  @Override
  protected ChangeValueMapEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new GridLayout(2, 1, 5, 5));
        final JTextField txtArtists = createEntityLookupField(ALBUM_ARTISTID_FK);
        txtArtists.setColumns(18);
        setDefaultFocusComponent(txtArtists);
        final JTextField txtTitle = createTextField(ALBUM_TITLE);
        txtTitle.setColumns(18);
        add(createPropertyPanel(ALBUM_ARTISTID_FK, txtArtists));
        add(createPropertyPanel(ALBUM_TITLE, txtTitle));
      }
    };
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(TrackModel.class, TrackPanel.class));
  }
}
