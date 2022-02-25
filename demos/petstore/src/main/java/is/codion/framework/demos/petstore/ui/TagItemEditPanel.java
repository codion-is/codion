/*
 * Copyright (c) 2004 - 2022, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.framework.demos.petstore.domain.Petstore.Tag;
import is.codion.framework.demos.petstore.domain.Petstore.TagItem;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.common.ui.panel.Panels;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;

public class TagItemEditPanel extends EntityEditPanel {

  public TagItemEditPanel(SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(Layouts.flexibleGridLayout(2, 1));
    EntityComboBox itemBox = createForeignKeyComboBox(TagItem.ITEM_FK)
            .popupWidth(240)
            .preferredWidth(180)
            .build();
    setInitialFocusComponent(itemBox);
    addInputPanel(TagItem.ITEM_FK);
    EntityComboBox itemTagBox = createForeignKeyComboBox(TagItem.TAG_FK)
            .build();
    addInputPanel(TagItem.TAG_FK, Panels.createEastButtonPanel(itemTagBox,
            EntityPanel.builder(Tag.TYPE).editPanelClass(TagEditPanel.class)
                    .createEditPanelAction(itemTagBox)));
  }
}
