/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import static org.jminor.framework.demos.chinook.domain.Chinook.ARTIST_NAME;

import javax.swing.JTextField;
import java.awt.GridLayout;

public class ArtistPanel extends EntityEditPanel {

  public ArtistPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusComponentKey(ARTIST_NAME);
    final JTextField txtName = createTextField(ARTIST_NAME);
    txtName.setColumns(18);

    setLayout(new GridLayout(1, 1, 5, 5));
    add(createPropertyPanel(ARTIST_NAME, txtName));
  }
}