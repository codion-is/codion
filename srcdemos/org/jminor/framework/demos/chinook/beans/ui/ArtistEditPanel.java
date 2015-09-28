/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.swing.model.EntityEditModel;
import org.jminor.framework.swing.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.ARTIST_NAME;

public class ArtistEditPanel extends EntityEditPanel {

  public ArtistEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(ARTIST_NAME);
    final JTextField txtName = createTextField(ARTIST_NAME);
    txtName.setColumns(18);

    setLayout(new GridLayout(1, 1, 5, 5));
    addPropertyPanel(ARTIST_NAME);
  }
}