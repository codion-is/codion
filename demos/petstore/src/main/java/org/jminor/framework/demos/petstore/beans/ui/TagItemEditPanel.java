/*
 * Copyright (c) 2004 - 2016, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;
import org.jminor.swing.framework.ui.EntityUiUtil;

import java.awt.Dimension;

import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_ITEM_FK;
import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_TAG_FK;

public class TagItemEditPanel extends EntityEditPanel {

  public TagItemEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(2,1,5,5));
    EntityComboBox box = createForeignKeyComboBox(TAG_ITEM_ITEM_FK);
    setInitialFocusComponent(box);
    box.setPopupWidth(240);
    box.setPreferredSize(new Dimension(180, UiUtil.getPreferredTextFieldHeight()));
    addPropertyPanel(TAG_ITEM_ITEM_FK);
    box = createForeignKeyComboBox(TAG_ITEM_TAG_FK);
    add(createPropertyPanel(TAG_ITEM_TAG_FK, EntityUiUtil.createEastButtonPanel(box,
            createEditPanelAction(box, new EntityPanelProvider(Petstore.T_TAG).setEditPanelClass(TagEditPanel.class)), false)));
  }
}
