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
 * Copyright (c) 2004 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.model.CancelException;
import is.codion.common.model.preferences.UserPreferences;
import is.codion.common.state.State;
import is.codion.common.user.User;
import is.codion.demos.chinook.domain.api.Chinook;
import is.codion.demos.chinook.model.ArtistTableModel;
import is.codion.demos.chinook.model.ChinookAppModel;
import is.codion.demos.chinook.model.TrackTableModel;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.plugin.flatlaf.intellij.themes.materialtheme.MaterialTheme;
import is.codion.plugin.swing.mcp.SwingMcpPlugin;
import is.codion.swing.common.ui.component.combobox.Completion;
import is.codion.swing.common.ui.component.indicator.ValidIndicatorFactory;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.component.table.FilterTableCellRenderer;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityEditPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityPanel.WindowType;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.ReferentialIntegrityErrorHandling;
import is.codion.swing.framework.ui.TabbedDetailLayout;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.ButtonGroup;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JTable;
import javax.swing.SwingConstants;
import java.awt.Dimension;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.ResourceBundle;

import static is.codion.demos.chinook.domain.api.Chinook.*;
import static is.codion.swing.common.ui.component.Components.gridLayoutPanel;
import static is.codion.swing.common.ui.component.Components.radioButton;
import static is.codion.swing.common.ui.key.KeyEvents.keyStroke;
import static is.codion.swing.framework.ui.EntityPanel.PanelState.HIDDEN;
import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.util.ResourceBundle.getBundle;
import static javax.swing.JOptionPane.showMessageDialog;

public final class ChinookAppPanel extends EntityApplicationPanel<ChinookAppModel> {

	private static final String LANGUAGE_PREFERENCES_KEY = ChinookAppPanel.class.getSimpleName() + ".language";
	private static final String LANGUAGE_IS = "is";
	private static final String LANGUAGE_EN = "en";
	private static final Locale LOCALE_IS = new Locale(LANGUAGE_IS, "IS");
	private static final Locale LOCALE_EN = new Locale(LANGUAGE_EN, "EN");

	/* Non-static so this is not initialized before main(), which sets the locale */
	private final ResourceBundle bundle = getBundle(ChinookAppPanel.class.getName());

	private final State mcpServerController = SwingMcpPlugin.mcpServer(this);

	public ChinookAppPanel(ChinookAppModel applicationModel) {
		super(applicationModel, createPanels(applicationModel), createLookupPanelBuilders());
	}

	private static List<EntityPanel> createPanels(ChinookAppModel applicationModel) {
		return List.of(
						new CustomerPanel(applicationModel.entityModels().get(Customer.TYPE)),
						new AlbumPanel(applicationModel.entityModels().get(Album.TYPE)),
						new PlaylistPanel(applicationModel.entityModels().get(Playlist.TYPE)));
	}

	private static List<EntityPanel.Builder> createLookupPanelBuilders() {
		EntityPanel.Builder genrePanelBuilder = EntityPanel.builder()
						.entityType(Genre.TYPE)
						.panel(ChinookAppPanel::createGenrePanel);

		EntityPanel.Builder mediaTypePanelBuilder = EntityPanel.builder()
						.entityType(MediaType.TYPE)
						.panel(ChinookAppPanel::createMediaTypePanel);

		EntityPanel.Builder artistPanelBuilder = EntityPanel.builder()
						.entityType(Artist.TYPE)
						.panel(ChinookAppPanel::createArtistPanel);

		EntityPanel.Builder employeePanelBuilder = EntityPanel.builder()
						.entityType(Employee.TYPE)
						.panel(ChinookAppPanel::createEmployeePanel);

		EntityPanel.Builder preferencesPanelBuilder = EntityPanel.builder()
						.entityType(Preferences.TYPE)
						.panel(ChinookAppPanel::createPreferencesPanel);

		return List.of(artistPanelBuilder, genrePanelBuilder, mediaTypePanelBuilder, employeePanelBuilder, preferencesPanelBuilder);
	}

	private static EntityPanel createGenrePanel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel genreModel = new SwingEntityModel(Genre.TYPE, connectionProvider);
		SwingEntityModel trackModel = new SwingEntityModel(new TrackTableModel(connectionProvider));
		genreModel.detailModels().add(trackModel);
		genreModel.tableModel().items().refresh();

		EntityPanel genrePanel = new EntityPanel(genreModel,
						new GenreEditPanel(genreModel.editModel()), config ->
						config.detailLayout(entityPanel -> TabbedDetailLayout.builder()
										.panel(entityPanel)
										.initialDetailState(HIDDEN)
										.build()));
		genrePanel.detailPanels().add(new EntityPanel(trackModel));

		return genrePanel;
	}

	private static EntityPanel createMediaTypePanel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel mediaTypeModel = new SwingEntityModel(MediaType.TYPE, connectionProvider);
		mediaTypeModel.tableModel().items().refresh();

		return new EntityPanel(mediaTypeModel, new MediaTypeEditPanel(mediaTypeModel.editModel()));
	}

	private static EntityPanel createArtistPanel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel artistModel = new SwingEntityModel(new ArtistTableModel(connectionProvider));
		artistModel.tableModel().items().refresh();

		return new EntityPanel(artistModel,
						new ArtistEditPanel(artistModel.editModel()),
						new ArtistTablePanel(artistModel.tableModel()));
	}

	private static EntityPanel createEmployeePanel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel employeeModel = new SwingEntityModel(Employee.TYPE, connectionProvider);
		SwingEntityModel customerModel = new SwingEntityModel(Customer.TYPE, connectionProvider);
		employeeModel.detailModels().add(customerModel);
		employeeModel.tableModel().items().refresh();

		EntityPanel employeePanel = new EntityPanel(employeeModel,
						new EmployeeTablePanel(employeeModel.tableModel()), config -> config
						.detailLayout(entityPanel -> TabbedDetailLayout.builder()
										.panel(entityPanel)
										.initialDetailState(HIDDEN)
										.build()));
		EntityPanel customerPanel = new EntityPanel(customerModel,
						new CustomerTablePanel(customerModel.tableModel()));
		employeePanel.detailPanels().add(customerPanel);
		employeePanel.setPreferredSize(new Dimension(1000, 500));

		return employeePanel;
	}

	private static EntityPanel createPreferencesPanel(EntityConnectionProvider connectionProvider) {
		SwingEntityModel preferencesModel = new SwingEntityModel(Preferences.TYPE, connectionProvider);
		preferencesModel.editModel().initializeComboBoxModels(Preferences.PREFERRED_GENRE_FK);
		preferencesModel.tableModel().items().refresh();

		return new EntityPanel(preferencesModel,
						new PreferencesEditPanel(preferencesModel.editModel()));
	}

	@Override
	protected Optional<Controls> createToolsMenuControls() {
		return super.createToolsMenuControls()
						.map(controls -> controls.copy()
										.control(Control.builder()
														.toggle(mcpServerController)
														.caption("MCP Server"))
										.build());
	}

	@Override
	protected Optional<Controls> createViewMenuControls() {
		return super.createViewMenuControls()
						.map(controls -> controls.copy()
										.controlAt(2, Control.builder()
														.command(this::selectLanguage)
														.caption(bundle.getString("language"))
														.build())
										.build());
	}

	private void selectLanguage() {
		String currentLanguage = UserPreferences.get(LANGUAGE_PREFERENCES_KEY, Locale.getDefault().getLanguage());
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
		showMessageDialog(this, languagePanel, bundle.getString("language_title"), JOptionPane.QUESTION_MESSAGE);
		String selectedLanguage = isButton.isSelected() ? LANGUAGE_IS : LANGUAGE_EN;
		if (!currentLanguage.equals(selectedLanguage)) {
			UserPreferences.set(LANGUAGE_PREFERENCES_KEY, selectedLanguage);
			showMessageDialog(this, bundle.getString("language_has_been_changed"));
		}
	}

	public static void main(String[] args) throws CancelException {
		String language = UserPreferences.get(LANGUAGE_PREFERENCES_KEY, Locale.getDefault().getLanguage());
		Locale.setDefault(LANGUAGE_IS.equals(language) ? LOCALE_IS : LOCALE_EN);
		FrameworkIcons.instance().add(Foundation.PLUS, Foundation.MINUS);
		Completion.COMPLETION_MODE.set(Completion.Mode.AUTOCOMPLETE);
		EntityApplicationPanel.CACHE_ENTITY_PANELS.set(true);
		EntityPanel.Config.TOOLBAR_CONTROLS.set(true);
		EntityPanel.Config.WINDOW_TYPE.set(WindowType.FRAME);
		EntityEditPanel.Config.MODIFIED_WARNING.set(true);
		// Add a CTRL modifier to the DELETE key shortcut for table panels
		EntityTablePanel.ControlKeys.DELETE.defaultKeystroke().map(keyStroke ->
						keyStroke(keyStroke.getKeyCode(), CTRL_DOWN_MASK));
		EntityTablePanel.Config.COLUMN_SELECTION.set(EntityTablePanel.ColumnSelection.MENU);
		EntityTablePanel.Config.AUTO_RESIZE_MODE_SELECTION.set(EntityTablePanel.AutoResizeModeSelection.MENU);
		EntityTablePanel.Config.INCLUDE_FILTERS.set(true);
		FilterTable.AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
		FilterTableCellRenderer.NUMERICAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
		FilterTableCellRenderer.TEMPORAL_HORIZONTAL_ALIGNMENT.set(SwingConstants.CENTER);
		ValidIndicatorFactory.FACTORY_CLASS.set("is.codion.plugin.flatlaf.indicator.FlatLafValidIndicatorFactory");
		ReferentialIntegrityErrorHandling.REFERENTIAL_INTEGRITY_ERROR_HANDLING
						.set(ReferentialIntegrityErrorHandling.DISPLAY_DEPENDENCIES);
		EntityApplicationPanel.builder(ChinookAppModel.class, ChinookAppPanel.class)
						.domain(Chinook.DOMAIN)
						.applicationName("Chinook")
						.applicationVersion(ChinookAppModel.VERSION)
						.defaultLookAndFeel(MaterialTheme.class)
						.defaultUser(User.parse("scott:tiger"))
						.start();
	}
}
