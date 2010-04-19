/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.chinook.beans.PlaylistTrackModel;
import org.jminor.framework.demos.chinook.domain.Chinook;

import javax.swing.JTextField;
import java.awt.FlowLayout;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class PlaylistPanel extends EntityPanel {

  public PlaylistPanel(final EntityModel model) {
    super(model, "Playlists");
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new FlowLayout(FlowLayout.LEADING));
        final JTextField txtName = createTextField(Chinook.PLAYLIST_NAME);
        txtName.setColumns(12);
        setDefaultFocusComponent(txtName);
        add(createPropertyPanel(Chinook.PLAYLIST_NAME, txtName));
      }
    };
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(PlaylistTrackModel.class, PlaylistTrackPanel.class));
  }
}