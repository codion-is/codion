/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityEditPanel;

import javax.swing.JTextField;
import java.awt.GridLayout;

import static org.jminor.framework.demos.chinook.domain.Chinook.MEDIATYPE_NAME;

public class MediaTypeEditPanel extends EntityEditPanel {

  public MediaTypeEditPanel(final EntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(MEDIATYPE_NAME);
    final JTextField txtName = createTextField(MEDIATYPE_NAME);
    txtName.setColumns(12);

    setLayout(new GridLayout(1, 1, 5, 5));
    addPropertyPanel(MEDIATYPE_NAME);
  }
}