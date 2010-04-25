/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.model.valuemap.ChangeValueMapEditModel;
import org.jminor.common.ui.UiUtil;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityComboBox;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.client.ui.EntityUiUtil;
import org.jminor.framework.demos.petstore.beans.TagModel;
import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_ITEM_FK;
import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_ITEM_TAG_FK;

import java.awt.Dimension;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 23:36:04
 */
public class TagItemPanel extends EntityPanel {

  public TagItemPanel(final EntityModel model) {
    super(model, "Item Tags");
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new FlexibleGridLayout(2,1,5,5));
        EntityComboBox box = createEntityComboBox(TAG_ITEM_ITEM_FK);
        setDefaultFocusComponent(box);
        box.setPopupWidth(240);
        box.setPreferredSize(new Dimension(180, UiUtil.getPreferredTextFieldHeight()));
        add(createPropertyPanel(TAG_ITEM_ITEM_FK, box));
        box = createEntityComboBox(TAG_ITEM_TAG_FK);
        add(createPropertyPanel(TAG_ITEM_TAG_FK, EntityUiUtil.createEntityComboBoxNewRecordPanel(box,
                new EntityPanelProvider("Tags", TagModel.class, TagPanel.class), false)));
      }
    };
  }
}
