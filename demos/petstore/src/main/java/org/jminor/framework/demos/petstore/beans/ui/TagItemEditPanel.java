/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.framework.demos.petstore.domain.Petstore;
import org.jminor.swing.common.ui.UiUtil;
import org.jminor.swing.common.ui.layout.FlexibleGridLayout;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityComboBox;
import org.jminor.swing.framework.ui.EntityEditPanel;
import org.jminor.swing.framework.ui.EntityPanelProvider;

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
    UiUtil.setPreferredWidth(itemBox, 180);
    addPropertyPanel(TAG_ITEM_ITEM_FK);
    final EntityComboBox itemTagBox = createForeignKeyComboBox(TAG_ITEM_TAG_FK);
    add(createPropertyPanel(TAG_ITEM_TAG_FK, UiUtil.createEastButtonPanel(itemTagBox,
            createEditPanelAction(itemTagBox, new EntityPanelProvider(Petstore.T_TAG)
                    .setEditPanelClass(TagEditPanel.class)), false)));
  }
}
