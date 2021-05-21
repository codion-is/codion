package is.codion.framework.demos.world.ui;

import is.codion.framework.demos.world.domain.api.World.CountryLanguage;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.ui.EntityEditPanel;

import static is.codion.swing.common.ui.layout.Layouts.gridLayout;

public final class CountryLanguageEditPanel extends EntityEditPanel {

  public CountryLanguageEditPanel(SwingEntityEditModel editModel) {
    super(editModel);
  }

  @Override
  protected void initializeUI() {
    setInitialFocusAttribute(CountryLanguage.COUNTRY_FK);

    foreignKeyComboBoxBuilder(CountryLanguage.COUNTRY_FK).preferredWidth(120).build();
    textFieldBuilder(CountryLanguage.LANGUAGE).columns(12).build();
    checkBoxBuilder(CountryLanguage.IS_OFFICIAL).includeCaption(false).build();
    textFieldBuilder(CountryLanguage.PERCENTAGE).build();

    setLayout(gridLayout(2, 4));

    addInputPanel(CountryLanguage.COUNTRY_FK);
    addInputPanel(CountryLanguage.LANGUAGE);
    addInputPanel(CountryLanguage.IS_OFFICIAL);
    addInputPanel(CountryLanguage.PERCENTAGE);
  }
}
