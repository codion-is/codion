/*
 * This file is part of Codion.
 *
 * Codion is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Codion is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Codion.  If not, see <https://www.gnu.org/licenses/>.
 *
 * Copyright (c) 2004 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.framework.demos.chinook.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.UserPreferences;
import is.codion.common.user.User;
import is.codion.framework.demos.chinook.domain.Chinook;
import is.codion.framework.demos.chinook.model.ChinookAppModel;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.table.FilteredTable;
import is.codion.swing.common.ui.component.table.FilteredTableCellRenderer;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanel.PanelState;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;
import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.ResourceBundle;

import static is.codion.framework.demos.chinook.domain.Chinook.*;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.radioButton;
import static is.codion.swing.common.ui.key.KeyboardShortcuts.keyStroke;
import static is.codion.swing.framework.ui.TabbedDetailLayout.panelState;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static javax.swing.JOptionPane.showMessageDialog;

public final class ChinookAppPanel extends EntityApplicationPanel<ChinookAppModel> {

  private static final String DEFAULT_FLAT_LOOK_AND_FEEL = "com.formdev.flatlaf.intellijthemes.materialthemeuilite.FlatMaterialDarkerIJTheme";
  private static final String LANGUAGE_PREFERENCES_KEY = ChinookAppPanel.class.getSimpleName() + ".language";
  private static final Locale LOCALE_IS = new Locale("is", "IS");
  private static final Locale LOCALE_EN = new Locale("en", "EN");
  private static final String LANGUAGE_IS = "is";
  private static final String LANGUAGE_EN = "en";

  private static final String SELECT_LANGUAGE = "select_language";

  /* Non-static so this is not initialized before main(), which sets the locale */
  private final ResourceBundle bundle = ResourceBundle.getBundle(ChinookAppPanel.class.getName());

  public ChinookAppPanel(ChinookAppModel applicationModel) {
    super(applicationModel);
    FrameworkIcons.instance().add(Foundation.PLUS, Foundation.MINUS);
  }

  @Override
  protected List<EntityPanel> createEntityPanels() {
    return Arrays.asList(
            new CustomerPanel(applicationModel().entityModel(Customer.TYPE)),
            new AlbumPanel(applicationModel().entityModel(Album.TYPE)),
            new PlaylistPanel(applicationModel().entityModel(Playlist.TYPE))
    );
  }

  @Override
  protected List<EntityPanel.Builder> createSupportEntityPanelBuilders() {
    EntityPanel.Builder trackPanelBuilder =
            EntityPanel.builder(Track.TYPE)
                    .tablePanel(TrackTablePanel.class);

    SwingEntityModel.Builder genreModelBuilder =
            SwingEntityModel.builder(Genre.TYPE)
                    .detailModel(SwingEntityModel.builder(Track.TYPE));

    EntityPanel.Builder genrePanelBuilder =
            EntityPanel.builder(genreModelBuilder)
                    .editPanel(GenreEditPanel.class)
                    .detailPanel(trackPanelBuilder)
                    .detailLayout(panelState(PanelState.HIDDEN));

    SwingEntityModel.Builder mediaTypeModelBuilder =
            SwingEntityModel.builder(MediaType.TYPE)
                    .detailModel(SwingEntityModel.builder(Track.TYPE));

    EntityPanel.Builder mediaTypePanelBuilder =
            EntityPanel.builder(mediaTypeModelBuilder)
                    .editPanel(MediaTypeEditPanel.class)
                    .detailPanel(trackPanelBuilder)
                    .detailLayout(panelState(PanelState.HIDDEN));

    EntityPanel.Builder artistPanelBuilder =
            EntityPanel.builder(Artist.TYPE)
                    .editPanel(ArtistEditPanel.class);

    EntityPanel.Builder customerPanelBuilder =
            EntityPanel.builder(Customer.TYPE)
                    .editPanel(CustomerEditPanel.class)
                    .tablePanel(CustomerTablePanel.class);

    SwingEntityModel.Builder employeeModelBuilder =
            SwingEntityModel.builder(Employee.TYPE)
                    .detailModel(SwingEntityModel.builder(Customer.TYPE));

    EntityPanel.Builder employeePanelBuilder =
            EntityPanel.builder(employeeModelBuilder)
                    .editPanel(EmployeeEditPanel.class)
                    .detailPanel(customerPanelBuilder)
                    .detailLayout(panelState(PanelState.HIDDEN))
                    .preferredSize(new Dimension(1000, 500));

    return Arrays.asList(artistPanelBuilder, genrePanelBuilder, mediaTypePanelBuilder, employeePanelBuilder);
  }

  @Override
  protected Controls createViewMenuControls() {
    return super.createViewMenuControls()
            .addAt(2, Control.builder(this::selectLanguage)
                    .name(bundle.getString(SELECT_LANGUAGE))
                    .build());
  }

  private void selectLanguage() {
    String currentLanguage = UserPreferences.getUserPreference(LANGUAGE_PREFERENCES_KEY, Locale.getDefault().getLanguage());
    JPanel languagePanel = gridLayoutPanel(2, 1).build();
    ButtonGroup buttonGroup = new ButtonGroup();
    radioButton()
            .text(bundle.getString("english"))
            .selected(currentLanguage.equals(LANGUAGE_EN))
            .buttonGroup(buttonGroup)
            .build(languagePanel::add);
    JRadioButton isButton = radioButton()
            .text(bundle.getString("icelandic"))
            .selected(currentLanguage.equals(LANGUAGE_IS))
            .buttonGroup(buttonGroup)
            .build(languagePanel::add);
    showMessageDialog(this, languagePanel, bundle.getString("language"), JOptionPane.QUESTION_MESSAGE);
    String selectedLanguage = isButton.isSelected() ? LANGUAGE_IS : LANGUAGE_EN;
    if (!currentLanguage.equals(selectedLanguage)) {
      UserPreferences.setUserPreference(LANGUAGE_PREFERENCES_KEY, selectedLanguage);
      showMessageDialog(this, bundle.getString("language_has_been_changed"));
    }
  }

  public static void main(String[] args) throws CancelException {
    String language = UserPreferences.getUserPreference(LANGUAGE_PREFERENCES_KEY, Locale.getDefault().getLanguage());
    Locale.setDefault(LANGUAGE_IS.equals(language) ? LOCALE_IS : LOCALE_EN);
    Arrays.stream(FlatAllIJThemes.INFOS)
            .forEach(LookAndFeelProvider::addLookAndFeelProvider);
    Completion.COMBO_BOX_COMPLETION_MODE.set(Completion.Mode.AUTOCOMPLETE);
    EntityApplicationPanel.CACHE_ENTITY_PANELS.set(true);
    EntityPanel.Config.TOOLBAR_CONTROLS.set(true);
    EntityPanel.Config.USE_FRAME_PANEL_DISPLAY.set(true);
    // Add a CTRL modifier to the DELETE key shortcut for table panels
    EntityTablePanel.Config.KEYBOARD_SHORTCUTS.keyStroke(EntityTablePanel.KeyboardShortcut.DELETE_SELECTED)
            .map(keyStroke -> keyStroke(keyStroke.getKeyCode(), CTRL_DOWN_MASK));
    FilteredTable.AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
    EntityTablePanel.Config.COLUMN_SELECTION.set(EntityTablePanel.ColumnSelection.MENU);
    FilteredTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
    FilteredTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
    ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING
            .set(ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES);
    EntityApplicationPanel.builder(ChinookAppModel.class, ChinookAppPanel.class)
            .applicationName("Chinook")
            .applicationVersion(ChinookAppModel.VERSION)
            .domainType(Chinook.DOMAIN)
            .defaultLookAndFeelClassName(DEFAULT_FLAT_LOOK_AND_FEEL)
            .defaultLoginUser(User.parse("scott:tiger"))
            .start();
  }
}
