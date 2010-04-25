/*
 * Copyright (c) 2004 - 2010, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.chinook.beans.ui;

import org.jminor.common.model.ChangeValueMapEditModel;
import org.jminor.framework.client.model.EntityEditModel;
import org.jminor.framework.client.model.EntityModel;
import org.jminor.framework.client.ui.EntityEditPanel;
import org.jminor.framework.client.ui.EntityPanel;
import org.jminor.framework.client.ui.EntityPanelProvider;
import org.jminor.framework.demos.chinook.beans.TrackModel;
import static org.jminor.framework.demos.chinook.domain.Chinook.GENRE_NAME;

import javax.swing.JTextField;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;

/**
 * User: Björn Darri
 * Date: 18.4.2010
 * Time: 20:05:39
 */
public class GenrePanel extends EntityPanel {

  public GenrePanel(final EntityModel model) {
    super(model, "Genres", true, false, HIDDEN);
  }

  @Override
  protected EntityEditPanel initializeEditPanel(final ChangeValueMapEditModel editModel) {
    return new EntityEditPanel((EntityEditModel) editModel) {
      @Override
      protected void initializeUI() {
        setLayout(new GridLayout(1, 1, 5, 5));
        final JTextField txtName = createTextField(GENRE_NAME);
        txtName.setColumns(12);
        setDefaultFocusComponent(txtName);
        add(createPropertyPanel(GENRE_NAME, txtName));
      }
    };
  }

  @Override
  protected List<EntityPanelProvider> getDetailPanelProviders() {
    return Arrays.asList(new EntityPanelProvider(TrackModel.class, TrackPanel.class));
  }
}