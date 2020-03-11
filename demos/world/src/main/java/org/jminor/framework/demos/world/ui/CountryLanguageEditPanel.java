package org.jminor.framework.demos.world.ui;

import org.jminor.framework.demos.world.domain.World;
import org.jminor.swing.framework.model.SwingEntityEditModel;
import org.jminor.swing.framework.ui.EntityEditPanel;

import java.awt.GridLayout;

import static org.jminor.swing.common.ui.Components.setPreferredWidth;

public final class CountryLanguageEditPanel extends EntityEditPanel {

  public CountryLanguageEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusProperty(World.COUNTRYLANGUAGE_COUNTRY_FK);

    setPreferredWidth(createForeignKeyComboBox(World.COUNTRYLANGUAGE_COUNTRY_FK), 120);
    createTextField(World.COUNTRYLANGUAGE_LANGUAGE).setColumns(12);
    createCheckBox(World.COUNTRYLANGUAGE_ISOFFICIAL, null, false);
    createTextField(World.COUNTRYLANGUAGE_PERCENTAGE);

    setLayout(new GridLayout(2, 4, 5, 5));

    addPropertyPanel(World.COUNTRYLANGUAGE_COUNTRY_FK);
    addPropertyPanel(World.COUNTRYLANGUAGE_LANGUAGE);
    addPropertyPanel(World.COUNTRYLANGUAGE_ISOFFICIAL);
    addPropertyPanel(World.COUNTRYLANGUAGE_PERCENTAGE);
  }
}
