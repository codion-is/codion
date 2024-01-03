/*
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.petstore.ui;

import is.codion.framework.demos.petstore.domain.Petstore.Tag;
import is.codion.framework.demos.petstore.domain.Petstore.TagItem;
import is.codion.swing.common.ui.layout.Layouts;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

public class TagItemEditPanel extends EntityEditPanel {

  public TagItemEditPanel(SwingEntityEditModel model) {
    super(model);
  }

  @Override
  protected void initializeUI() {
    initialFocusAttribute().set(TagItem.ITEM_FK);
    createForeignKeyComboBox(TagItem.ITEM_FK)
            .preferredWidth(180);
    createForeignKeyComboBoxPanel(TagItem.TAG_FK, this::createTagEditPanel)
            .add(true);
    setLayout(Layouts.flexibleGridLayout(2, 1));
    addInputPanel(TagItem.ITEM_FK);
    addInputPanel(TagItem.TAG_FK);
  }

  private TagEditPanel createTagEditPanel() {
    return new TagEditPanel(new SwingEntityEditModel(Tag.TYPE, editModel().connectionProvider()));
  }
}
