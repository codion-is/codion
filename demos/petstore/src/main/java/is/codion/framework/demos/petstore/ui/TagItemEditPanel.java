/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.framework.demos.petstore.domain.Petstore.Tag;
import is.codion.framework.demos.petstore.domain.Petstore.TagItem;
import is.codion.swing.common.ui.Components;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityComboBox;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanelBuilder;

public class TagItemEditPanel extends EntityEditPanel {

  public TagItemEditPanel(final SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    setLayout(Layouts.flexibleGridLayout(2, 1));
    final EntityComboBox itemBox = createForeignKeyComboBox(TagItem.ITEM_FK);
    setInitialFocusComponent(itemBox);
    itemBox.setPopupWidth(240);
    Components.setPreferredWidth(itemBox, 180);
    addInputPanel(TagItem.ITEM_FK);
    final EntityComboBox itemTagBox = createForeignKeyComboBox(TagItem.TAG_FK);
    add(createInputPanel(TagItem.TAG_FK, Components.createEastButtonPanel(itemTagBox,
            new EntityPanelBuilder(Tag.TYPE).setEditPanelClass(TagEditPanel.class)
                    .createEditPanelAction(itemTagBox))));
  }
}
