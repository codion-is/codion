/*
 * Copyright (c) 2004 - 2020, Björn Darri Sigurðsson. All Rights Reserved.
 */
package org.jminor.framework.demos.world.ui;

import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static org.jminor.swing.common.ui.Components.setPreferredWidth;

public final class CountryLanguageEditPanel extends EntityEditPanel {

  private static final int COMBOBOX_WIDTH = 120;
  private static final int TEXT_FIELD_COLUMNS = 12;

  public CountryLanguageEditPanel(final SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(World.COUNTRYLANGUAGE_COUNTRY_FK);

    setPreferredWidth(createForeignKeyComboBox(World.COUNTRYLANGUAGE_COUNTRY_FK), COMBOBOX_WIDTH);
    createTextField(World.COUNTRYLANGUAGE_LANGUAGE).setColumns(TEXT_FIELD_COLUMNS);
    createCheckBox(World.COUNTRYLANGUAGE_ISOFFICIAL, null, false);
    createTextField(World.COUNTRYLANGUAGE_PERCENTAGE);

    setLayout(new GridLayout(2, 4, 5, 5));

    addPropertyPanel(World.COUNTRYLANGUAGE_COUNTRY_FK);
    addPropertyPanel(World.COUNTRYLANGUAGE_LANGUAGE);
    addPropertyPanel(World.COUNTRYLANGUAGE_ISOFFICIAL);
    addPropertyPanel(World.COUNTRYLANGUAGE_PERCENTAGE);
  }
}
