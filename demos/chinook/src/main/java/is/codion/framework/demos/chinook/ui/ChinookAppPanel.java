/*
 * Copyright (c) 2004 - 2023, Björn Darri Sigurðsson. All Rights Reserved.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.model.table.ColumnConditionModel;
import is.codion.common.model.table.ColumnConditionModel.AutomaticWildcard;
import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.chinook.model.ChinookApplicationModel;
import is.codion.framework.demos.chinook.model.EmployeeTableModel;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.laf.LookAndFeelSelectionPanel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTableCellRenderer;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.addLookAndFeelProvider;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeelProvider;
import static is.codion.swing.framework.ui.EntityApplicationBuilder.entityApplicationBuilder;
import static javax.swing.JOptionPane.showMessageDialog;

public final class ChinookAppPanel extends EntityApplicationPanel<ChinookApplicationModel> {

  private static final String LANGUAGE_PREFERENCES_KEY = ChinookAppPanel.class.getSimpleName() + ".language";
  private static final Locale LOCALE_IS = new Locale("is", "IS");
  private static final Locale LOCALE_EN = new Locale("en", "EN");
  private static final String LANGUAGE_IS = "is";
  private static final String LANGUAGE_EN = "en";

  private static final String SELECT_LANGUAGE = "select_language";

  /* Non-static so this is not initialized before main(), which sets the locale */
  private final ResourceBundle bundle = ResourceBundle.getBundle(ChinookAppPanel.class.getName());

  public ChinookAppPanel(ChinookApplicationModel applicationModel) {
    super(applicationModel);
  }

  @Override
  protected List<EntityPanel> createEntityPanels() {
    return Arrays.asList(
            new CustomerPanel(model().entityModel(Customer.TYPE)),
            new ArtistPanel(model().entityModel(Artist.TYPE)),
            new PlaylistPanel(model().entityModel(Playlist.TYPE))
    );
  }

  @Override
  protected List<EntityPanel.Builder> createSupportEntityPanelBuilders() {
    EntityPanel.Builder trackBuilder =
            EntityPanel.builder(SwingEntityModel.builder(Track.TYPE))
                    .tablePanelClass(TrackTablePanel.class);

    EntityPanel.Builder customerBuilder =
            EntityPanel.builder(SwingEntityModel.builder(Customer.TYPE))
                    .editPanelClass(CustomerEditPanel.class)
                    .tablePanelClass(CustomerTablePanel.class);

    EntityPanel.Builder genreBuilder =
            EntityPanel.builder(SwingEntityModel.builder(Genre.TYPE)
                            .detailModelBuilder(SwingEntityModel.builder(Track.TYPE)))
                    .editPanelClass(GenreEditPanel.class)
                    .detailPanelBuilder(trackBuilder)
                    .detailPanelState(EntityPanel.PanelState.HIDDEN);

    EntityPanel.Builder mediaTypeBuilder =
            EntityPanel.builder(SwingEntityModel.builder(MediaType.TYPE)
                            .detailModelBuilder(SwingEntityModel.builder(Track.TYPE)))
                    .editPanelClass(MediaTypeEditPanel.class)
                    .detailPanelBuilder(trackBuilder)
                    .detailPanelState(EntityPanel.PanelState.HIDDEN);

    EntityPanel.Builder employeeBuilder =
            EntityPanel.builder(SwingEntityModel.builder(Employee.TYPE)
                            .detailModelBuilder(SwingEntityModel.builder(Customer.TYPE))
                            .tableModelClass(EmployeeTableModel.class))
                    .editPanelClass(EmployeeEditPanel.class)
                    .tablePanelClass(EmployeeTablePanel.class)
                    .detailPanelBuilder(customerBuilder)
                    .detailPanelState(EntityPanel.PanelState.HIDDEN)
                    .preferredSize(new Dimension(1000, 500));

    return Arrays.asList(genreBuilder, mediaTypeBuilder, employeeBuilder);
  }

  @Override
  protected Controls createViewMenuControls() {
    return super.createViewMenuControls()
            .addAt(4, Control.builder(this::selectLanguage)
                    .caption(bundle.getString(SELECT_LANGUAGE))
                    .build());
  }

  private void selectLanguage() {
    String language = UserPreferences.getUserPreference(LANGUAGE_PREFERENCES_KEY, Locale.getDefault().getLanguage());
    JRadioButton enButton = new JRadioButton("English", language.equals(LANGUAGE_EN));
    JRadioButton isButton = new JRadioButton("\u00cdslenska", language.equals(LANGUAGE_IS));
    ButtonGroup langButtonGroup = new ButtonGroup();
    langButtonGroup.add(enButton);
    langButtonGroup.add(isButton);
    JPanel buttonPanel = new JPanel(new GridLayout(2, 1, 5, 5));
    buttonPanel.add(enButton);
    buttonPanel.add(isButton);
    showMessageDialog(this, buttonPanel, "Language/Tungum\u00e1l", JOptionPane.QUESTION_MESSAGE);
    String newLanguage = isButton.isSelected() ? LANGUAGE_IS : LANGUAGE_EN;
    if (!language.equals(newLanguage)) {
      UserPreferences.setUserPreference(LANGUAGE_PREFERENCES_KEY, newLanguage);
      showMessageDialog(this,
              "Language has been changed, restart the application to apply the changes.\n\n" +
                      "Tungum\u00e1li hefur veri\u00f0 breytt, endurr\u00e6stu kerfi\u00f0 til að virkja breytingarnar.");
    }
  }

  public static void main(String[] args) throws CancelException {
    String language = UserPreferences.getUserPreference(LANGUAGE_PREFERENCES_KEY, Locale.getDefault().getLanguage());
    Locale.setDefault(LANGUAGE_IS.equals(language) ? LOCALE_IS : LOCALE_EN);
    LookAndFeelSelectionPanel.CHANGE_ON_SELECTION.set(true);
    Arrays.stream(FlatAllIJThemes.INFOS).forEach(themeInfo ->
            addLookAndFeelProvider(lookAndFeelProvider(themeInfo.getClassName())));
    Completion.COMBO_BOX_COMPLETION_MODE.set(Completion.Mode.AUTOCOMPLETE);
    EntityApplicationPanel.DISPLAY_ENTITY_PANELS_IN_FRAME.set(true);
    EntityApplicationPanel.PERSIST_ENTITY_PANELS.set(true);
    EntityPanel.TOOLBAR_BUTTONS.set(true);
    EntityPanel.USE_FRAME_PANEL_DISPLAY.set(true);
    EntityTablePanel.TABLE_AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    EntityTablePanel.COLUMN_SELECTION.set(EntityTablePanel.ColumnSelection.MENU);
    EntityTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
    EntityTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING
            .set(ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES);
    ColumnConditionModel.AUTOMATIC_WILDCARD.set(AutomaticWildcard.POSTFIX);
    ColumnConditionModel.CASE_SENSITIVE.set(false);
    EntityConnectionProvider.CLIENT_DOMAIN_CLASS.set("is.codion.framework.demos.chinook.domain.impl.ChinookImpl");
    SwingUtilities.invokeLater(() -> entityApplicationBuilder(ChinookApplicationModel.class, ChinookAppPanel.class)
            .applicationName("Chinook")
            .applicationVersion(ChinookApplicationModel.VERSION)
            .frameSize(new Dimension(1280, 720))
            .defaultLoginUser(User.parse("scott:tiger"))
            .start());

  }
}
