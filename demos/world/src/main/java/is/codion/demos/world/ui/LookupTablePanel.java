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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.world.ui;

import is.codion.common.state.State;
import is.codion.demos.world.domain.api.World.Lookup;
import is.codion.demos.world.domain.api.WorldObjectMapperFactory;
import is.codion.framework.domain.entity.Entity;
import is.codion.framework.json.domain.EntityObjectMapper;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.component.button.ToggleButtonType;
import is.codion.swing.common.ui.control.Control;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.common.ui.control.ToggleControl;
import is.codion.swing.common.ui.dialog.Dialogs;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.icon.FrameworkIcons;

import org.jxmapviewer.JXMapKit;
import org.kordamp.ikonli.foundation.Foundation;

import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.SwingConstants;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.BorderLayout;
import java.awt.Dimension;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import static is.codion.demos.world.ui.LookupTablePanel.ExportFormat.CSV;
import static is.codion.demos.world.ui.LookupTablePanel.ExportFormat.JSON;
import static is.codion.swing.common.ui.component.Components.scrollPane;
import static is.codion.swing.common.ui.component.Components.toolBar;
import static is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView.SIMPLE;
import static is.codion.swing.framework.ui.EntityTablePanel.ControlKeys.*;
import static java.util.Objects.requireNonNull;
import static java.util.stream.Collectors.toSet;

final class LookupTablePanel extends EntityTablePanel {

	private static final Dimension DEFAULT_MAP_SIZE = new Dimension(400, 400);
	private static final FrameworkIcons ICONS = FrameworkIcons.instance();

	enum ExportFormat {
		CSV {
			@Override
			public String defaultFileName() {
				return "export.csv";
			}
		},
		JSON {
			@Override
			public String defaultFileName() {
				return "export.json";
			}
		};

		public abstract String defaultFileName();
	}

	private final EntityObjectMapper objectMapper;

	private final State columnSelectionPanelVisible = State.state(true);
	private final State mapDialogVisible = State.builder()
					.consumer(this::setMapDialogVisible)
					.build();

	private final Control toggleMapControl = Control.builder()
					.toggle(mapDialogVisible)
					.icon(ICONS.get(Foundation.MAP))
					.caption("Show map")
					.build();
	private final JScrollPane columnSelectionScrollPane = scrollPane()
					.view(createColumnSelectionToolBar())
					.verticalUnitIncrement(16)
					.build();
	private final JXMapKit mapKit = Maps.createMapKit();

	private JDialog mapKitDialog;

	LookupTablePanel(SwingEntityTableModel lookupModel) {
		super(lookupModel, config -> config
						.conditionView(SIMPLE));
		columnSelectionPanelVisible.addConsumer(this::setColumnSelectionPanelVisible);
		objectMapper = new WorldObjectMapperFactory().entityObjectMapper(lookupModel.entities());
		objectMapper.setIncludeNullValues(false);
		table().setAutoResizeMode(JTable.AUTO_RESIZE_ALL_COLUMNS);
		configurePopupMenuAndToolBar();
		bindEvents();
	}

	@Override
	public void updateUI() {
		super.updateUI();
		Utilities.updateComponentTreeUI(mapKit);
	}

	@Override
	protected void setupControls() {
		control(CLEAR).set(Control.builder()
						.command(this::clearTableAndCondition)
						.caption("Clear")
						.mnemonic('C')
						.icon(ICONS.clear())
						.build());
	}

	@Override
	protected void layoutPanel(JComponent tableComponent, JPanel southPanel) {
		super.layoutPanel(tableComponent, southPanel);
		add(columnSelectionScrollPane, BorderLayout.EAST);
	}

	private void configurePopupMenuAndToolBar() {
		configurePopupMenu(layout -> layout.clear()
						.control(REFRESH)
						.control(CLEAR)
						.separator()
						.control(Controls.builder()
										.caption("Export")
										.icon(ICONS.get(Foundation.PAGE_EXPORT))
										.control(Control.builder()
														.command(this::exportCSV)
														.caption("CSV..."))
										.control(Control.builder()
														.command(this::exportJSON)
														.caption("JSON...")))
						.control(Controls.builder()
										.caption("Import")
										.icon(ICONS.get(Foundation.PAGE_ADD))
										.control(Control.builder()
														.command(this::importJSON)
														.caption("JSON...")))
						.separator()
						.control(toggleMapControl)
						.separator()
						.control(Controls.builder()
										.caption("Columns")
										.icon(FrameworkIcons.instance().columns())
										.control(Control.builder()
														.toggle(columnSelectionPanelVisible)
														.caption("Select")
														.build())
										.control(control(RESET_COLUMNS).get())
										.control(control(SELECT_AUTO_RESIZE_MODE).get()))
						.separator()
						.control(CONDITION_CONTROLS)
						.control(COPY_CONTROLS));

		configureToolBar(config -> config.clear()
						.control(toggleMapControl)
						.separator()
						.defaults());
	}

	private void bindEvents() {
		tableModel().items().visible().addListener(this::displayCityLocations);
		tableModel().selection().indexes().addListener(this::displayCityLocations);
	}

	private void displayCityLocations() {
		if (mapKit.isShowing()) {
			Collection<Entity> entities = tableModel().selection().empty().is() ?
							tableModel().items().visible().get() :
							tableModel().selection().items().get();
			Maps.paintWaypoints(entities.stream()
							.map(entity -> entity.optional(Lookup.CITY_LOCATION))
							.flatMap(Optional::stream)
							.collect(toSet()), mapKit.getMainMap());
		}
	}

	private void setMapDialogVisible(boolean visible) {
		if (mapKitDialog == null) {
			mapKitDialog = Dialogs.builder()
							.component(mapKit)
							.owner(this)
							.modal(false)
							.title("World Map")
							.size(DEFAULT_MAP_SIZE)
							.onShown(dialog -> displayCityLocations())
							.onClosed(e -> mapDialogVisible.set(false))
							.build();
		}
		mapKitDialog.setVisible(visible);
	}

	private void exportCSV() {
		export(CSV);
	}

	private void exportJSON() {
		export(JSON);
	}

	private void export(ExportFormat format) {
		File fileToSave = Dialogs.select()
						.files()
						.owner(this)
						.selectFileToSave(format.defaultFileName());
		Dialogs.progressWorker()
						.task(() -> export(fileToSave, format))
						.owner(this)
						.title("Exporting data")
						.onResult("Export successful")
						.onException("Export failed")
						.execute();
	}

	private void export(File file, ExportFormat format) throws IOException {
		requireNonNull(file);
		requireNonNull(format);
		switch (format) {
			case CSV:
				exportCSV(file);
				break;
			case JSON:
				exportJSON(file);
				break;
			default:
				throw new IllegalArgumentException("Unknown export format: " + format);
		}
	}

	private void exportCSV(File file) throws IOException {
		Files.write(file.toPath(), List.of(table().export()
						.delimiter(',')
						.selected(true)
						.get()));
	}

	private void exportJSON(File file) throws IOException {
		Collection<Entity> entities = tableModel().selection().empty().is() ?
						tableModel().items().get() :
						tableModel().selection().items().get();
		Files.writeString(file.toPath(), objectMapper.writeValueAsString(entities));
	}

	private void importJSON() throws IOException {
		importJSON(Dialogs.select()
						.files()
						.owner(this)
						.filter(new FileNameExtensionFilter("JSON", "json"))
						.selectFile());
	}

	public void importJSON(File file) throws IOException {
		List<Entity> entities = objectMapper.deserializeEntities(
						String.join("\n", Files.readAllLines(file.toPath())));
		clearTableAndCondition();
		tableModel().items().visible().add(0, entities);
	}

	private JToolBar createColumnSelectionToolBar() {
		Controls toggleColumnsControls = table().createToggleColumnsControls();

		return toolBar()
						.controls(Controls.builder()
										.control(createSelectAllColumnsControl(toggleColumnsControls))
										.separator()
										.actions(toggleColumnsControls.actions()))
						.floatable(false)
						.orientation(SwingConstants.VERTICAL)
						.toggleButtonType(ToggleButtonType.CHECKBOX)
						.includeButtonText(true)
						.build();
	}

	private void setColumnSelectionPanelVisible(boolean visible) {
		columnSelectionScrollPane.setVisible(visible);
		revalidate();
	}

	private void clearTableAndCondition() {
		tableModel().items().clear();
		tableModel().queryModel().condition().clear();
	}

	private static Control createSelectAllColumnsControl(Controls toggleColumnsControls) {
		return Control.builder()
						.command(() -> toggleColumnsControls.actions().stream()
										.map(ToggleControl.class::cast)
										.forEach(toggleControl -> toggleControl.value().set(true)))
						.caption("Select all")
						.icon(ICONS.get(Foundation.CHECK))
						.build();
	}
}
