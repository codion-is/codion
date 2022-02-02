package is.codion.framework.demos.world.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.world.domain.api.World.Continent;
import is.codion.framework.demos.world.domain.api.World.Lookup;
import is.codion.framework.demos.world.model.CountryModel;
import is.codion.framework.demos.world.model.WorldAppModel;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.EntityTablePanel.ColumnSelection;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import com.formdev.flatlaf.FlatDarkLaf;
import com.formdev.flatlaf.FlatLightLaf;

import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.util.List;
import java.util.Locale;

import static is.codion.swing.common.ui.laf.LookAndFeelProvider.addLookAndFeelProvider;
import static java.util.Arrays.asList;

public final class WorldAppPanel extends EntityApplicationPanel<WorldAppModel> {

  public WorldAppPanel() {
    super("World");
  }

  // tag::initializeEntityPanels[]
  @Override
  protected List<EntityPanel> initializeEntityPanels(WorldAppModel applicationModel) {
    CountryModel countryModel = applicationModel.getEntityModel(CountryModel.class);
    CountryPanel countryPanel = new CountryPanel(countryModel);

    SwingEntityModel continentModel = applicationModel.getEntityModel(Continent.TYPE);
    ContinentPanel continentPanel = new ContinentPanel(continentModel);

    SwingEntityModel lookupModel = applicationModel.getEntityModel(Lookup.TYPE);
    EntityPanel lookupPanel = new EntityPanel(lookupModel,
            new LookupTablePanel(lookupModel.getTableModel()));

    return asList(countryPanel, continentPanel, lookupPanel);
  }
  // end::initializeEntityPanels[]

  @Override
  protected WorldAppModel initializeApplicationModel(EntityConnectionProvider connectionProvider) {
    return new WorldAppModel(connectionProvider);
  }

  @Override
  protected String getDefaultSystemLookAndFeelName() {
    return FlatDarkLaf.class.getName();
  }

  public static void main(final String[] args) throws CancelException {
    Locale.setDefault(new Locale("en", "EN"));
    addLookAndFeelProvider(LookAndFeelProvider.create(FlatLightLaf.class.getName(), FlatLightLaf::setup));
    addLookAndFeelProvider(LookAndFeelProvider.create(FlatDarkLaf.class.getName(), FlatDarkLaf::setup));
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(AutomaticWildcard.PREFIX_AND_POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityTablePanel.COLUMN_SELECTION.set(ColumnSelection.MENU);
    EntityTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING.set(ReferentialIntegrityErrorHandling.DEPENDENCIES);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.world.domain.WorldImpl");
    SwingUtilities.invokeLater(() -> new WorldAppPanel().starter()
            .frameSize(new Dimension(1280, 720))
            .defaultLoginUser(User.parseUser("scott:tiger"))
            .start());
  }
}
