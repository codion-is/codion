/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.client.ui.EntityUiUtil;
import org.jminor.framework.demos.petstore.domain.Petstore;

import java.awt.Dimension;

import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_ITEM_FK;
import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_TAG_FK;

public class TagItemPanel extends EntityEditPanel {

  public TagItemPanel(final EntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(2,1,5,5));
    EntityComboBox box = createEntityComboBox(TAG_ITEM_ITEM_FK);
    setInitialFocusComponent(box);
    box.setPopupWidth(240);
    box.setPreferredSize(new Dimension(180, UiUtil.getPreferredTextFieldHeight()));
    addPropertyPanel(TAG_ITEM_ITEM_FK);
    box = createEntityComboBox(TAG_ITEM_TAG_FK);
    add(createPropertyPanel(TAG_ITEM_TAG_FK, EntityUiUtil.createEntityComboBoxPanel(box,
            new EntityPanelProvider(Petstore.T_TAG).setEditPanelClass(TagPanel.class), false)));
  }
}
