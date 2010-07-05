/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.ARTIST_NAME;

import javax.swing.JTextField;
import java.awt.GridLayout;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class ArtistPanel extends EntityEditPanel {

  public ArtistPanel(final EntityEditModel editModel) {
    super(editModel);
  }
//  public ArtistPanel(final EntityModel model) {
//    super(model, new ArtistEditPanel(model.getEditModel()));
//    addDetailPanel(new AlbumPanel(model.getDetailModel(T_ALBUM)));
//    setDetailSplitPanelResizeWeight(0.3);
//  }

  @Override
  protected void initializeUI() {
    setLayout(new GridLayout(1, 1, 5, 5));
    final JTextField txtName = createTextField(ARTIST_NAME);
    txtName.setColumns(18);
    setInitialFocusComponent(txtName);
    add(createPropertyPanel(ARTIST_NAME, txtName));
  }
}