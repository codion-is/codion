/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.ui;

import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.swing.common.ui.Components;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelBuilder;

import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_ITEM_FK;
import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_TAG_FK;

public class TagItemEditPanel extends EntityEditPanel {

  public TagItemEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(new FlexibleGridLayout(2, 1, 5, 5));
    final EntityComboBox itemBox = createForeignKeyComboBox(TAG_ITEM_ITEM_FK);
    setInitialFocusComponent(itemBox);
    itemBox.setPopupWidth(240);
    Components.setPreferredWidth(itemBox, 180);
    addPropertyPanel(TAG_ITEM_ITEM_FK);
    final EntityComboBox itemTagBox = createForeignKeyComboBox(TAG_ITEM_TAG_FK);
    add(createPropertyPanel(TAG_ITEM_TAG_FK, Components.createEastButtonPanel(itemTagBox,
            new EntityPanelBuilder(Petstore.T_TAG).setEditPanelClass(TagEditPanel.class)
                    .createEditPanelAction(itemTagBox))));
  }
}
