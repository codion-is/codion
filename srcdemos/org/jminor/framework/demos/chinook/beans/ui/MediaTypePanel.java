/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.chinook.beans.TrackModel;
import static org.jminor.framework.demos.chinook.domain.Chinook.MEDIATYPE_NAME;

import javax.swing.JTextField;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class MediaTypePanel extends EntityPanel {

  public MediaTypePanel(final EntityModel model) {
    super(model);
  }

  @Override
  protected void initialize() {
    setDetailPanelState(HIDDEN);
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final EntityEditModel editModel) {
    return new EntityEditPanel(editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new GridLayout(1, 1, 5, 5));
        final JTextField txtName = createTextField(MEDIATYPE_NAME);
        txtName.setColumns(12);
        setInitialFocusComponent(txtName);
        add(createPropertyPanel(MEDIATYPE_NAME, txtName));
      }
    };
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(TrackModel.class, TrackPanel.class));
  }
}