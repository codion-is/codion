/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.swing.ui.layout.FlexibleGridLayout;
import org.jminor.framework.swing.model.EntityEditModel;
import org.jminor.framework.swing.ui.EntityEditPanel;

import javax.swing.JTextField;

import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_TAG;

public class TagEditPanel extends EntityEditPanel {

  public TagEditPanel(final EntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(1,1,5,5));
    final JTextField txt = createTextField(TAG_TAG);
    setInitialFocusComponent(txt);
    txt.setColumns(16);
    addPropertyPanel(TAG_TAG);
  }
}