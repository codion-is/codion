package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
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
    setInitialFocusAttribute(CountryLanguage.COUNTRY_FK);

    setPreferredWidth(createForeignKeyComboBox(CountryLanguage.COUNTRY_FK), 120);
    createTextField(CountryLanguage.LANGUAGE).setColumns(12);
    createCheckBox(CountryLanguage.IS_OFFICIAL, null, IncludeCaption.NO);
    createTextField(CountryLanguage.PERCENTAGE);

    setLayout(gridLayout(2, 4));

    addInputPanel(CountryLanguage.COUNTRY_FK);
    addInputPanel(CountryLanguage.LANGUAGE);
    addInputPanel(CountryLanguage.IS_OFFICIAL);
    addInputPanel(CountryLanguage.PERCENTAGE);
  }
}
