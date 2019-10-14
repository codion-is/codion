/*
 * Copyright (c) 2004 - 2019, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.beans.ui;

import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static org.jminor.swing.common.ui.UiUtil.setPreferredWidth;

public final class CityEditPanel extends EntityEditPanel {

  private static final int COMBOBOX_WIDTH = 120;
  private static final int TEXT_FIELD_COLUMNS = 12;

  public CityEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(World.CITY_COUNTRY_FK);

    setPreferredWidth(createForeignKeyComboBox(World.CITY_COUNTRY_FK), COMBOBOX_WIDTH);
    createTextField(World.CITY_NAME).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(World.CITY_DISTRICT).setColumns(TEXT_FIELD_COLUMNS);
    createTextField(World.CITY_POPULATION);

    setLayout(new GridLayout(2, 2, 5, 5));

    addPropertyPanel(World.CITY_COUNTRY_FK);
    addPropertyPanel(World.CITY_NAME);
    addPropertyPanel(World.CITY_DISTRICT);
    addPropertyPanel(World.CITY_POPULATION);
  }
}
