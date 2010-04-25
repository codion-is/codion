/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.petstore.beans.ui;

import org.jminor.common.model.ChangeValueMapEditModel;
import org.jminor.common.ui.ChangeValueMapEditPanel;
import org.jminor.common.ui.layout.FlexibleGridLayout;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.petstore.beans.TagItemModel;
import static org.jminor.framework.demos.petstore.domain.Petstore.TAG_TAG;

import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

/**
 * User: Bjorn Darri
 * Date: 24.12.2007
 * Time: 14:05:58
 */
public class TagPanel extends EntityPanel {

  public TagPanel(final EntityModel model) {
    super(model, "Tags");
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(TagItemModel.class, TagItemPanel.class));
  }

  @Override
  protected ChangeValueMapEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new FlexibleGridLayout(1,1,5,5));
        final JTextField txt = createTextField(TAG_TAG);
        setDefaultFocusComponent(txt);
        txt.setColumns(16);
        add(createPropertyPanel(TAG_TAG, txt));
      }
    };
  }
}