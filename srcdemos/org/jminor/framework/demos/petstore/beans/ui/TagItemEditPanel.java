/*
 * Copyright (c) 2004 - 2015, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.swing.ui.UiUtil;
import org.jminor.common.swing.ui.layout.FlexibleGridLayout;
import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.framework.swing.model.EntityEditModel;
import org.jminor.framework.swing.ui.EntityComboBox;
import org.jminor.framework.swing.ui.EntityEditPanel;
import org.jminor.framework.swing.ui.EntityPanelProvider;
import org.jminor.framework.swing.ui.EntityUiUtil;

import java.awt.Dimension;

import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_ITEM_FK;
import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_TAG_FK;

public class TagItemEditPanel extends EntityEditPanel {

  public TagItemEditPanel(final EntityEditModel model) {
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
    add(createPropertyPanel(TAG_ITEM_TAG_FK, EntityUiUtil.createEastButtonPanel(box,
            createEditPanelAction(box, new EntityPanelProvider(Petstore.T_TAG).setEditPanelClass(TagEditPanel.class)), false)));
  }
}
