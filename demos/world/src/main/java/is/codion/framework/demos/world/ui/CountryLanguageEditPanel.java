package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.World;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityInputComponents.IncludeCaption;

import static is.codion.swing.common.ui.Components.setPreferredWidth;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class CountryLanguageEditPanel extends EntityEditPanel {

  public CountryLanguageEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(World.COUNTRYLANGUAGE_COUNTRY_FK);

    setPreferredWidth(createForeignKeyComboBox(World.COUNTRYLANGUAGE_COUNTRY_FK), 120);
    createTextField(World.COUNTRYLANGUAGE_LANGUAGE).setColumns(12);
    createCheckBox(World.COUNTRYLANGUAGE_ISOFFICIAL, null, IncludeCaption.NO);
    createTextField(World.COUNTRYLANGUAGE_PERCENTAGE);

    setLayout(gridLayout(2, 4));

    addPropertyPanel(World.COUNTRYLANGUAGE_COUNTRY_FK);
    addPropertyPanel(World.COUNTRYLANGUAGE_LANGUAGE);
    addPropertyPanel(World.COUNTRYLANGUAGE_ISOFFICIAL);
    addPropertyPanel(World.COUNTRYLANGUAGE_PERCENTAGE);
  }
}
