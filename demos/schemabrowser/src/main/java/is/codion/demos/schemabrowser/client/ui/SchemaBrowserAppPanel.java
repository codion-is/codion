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
package is.codion.demos.schemabrowser.client.ui;

import is.codion.common.user.User;
import is.codion.demos.schemabrowser.domain.SchemaBrowser;
import is.codion.demos.schemabrowser.domain.SchemaBrowser.Constraint;
import is.codion.demos.schemabrowser.domain.SchemaBrowser.ConstraintColumn;
import is.codion.demos.schemabrowser.domain.SchemaBrowser.Schema;
import is.codion.demos.schemabrowser.domain.SchemaBrowser.Table;
import is.codion.demos.schemabrowser.domain.SchemaBrowser.TableColumn;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionView;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.control.Controls;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplication;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.EntityTablePanel.ControlKeys;
import is.codion.swing.framework.ui.TabbedDetailLayout;
import is.codion.swing.framework.ui.WindowDetailLayout;

import javax.swing.JTable;
import java.util.List;

import static java.util.Arrays.asList;
import static java.util.Collections.emptyList;

public class SchemaBrowserAppPanel extends EntityApplicationPanel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

	public SchemaBrowserAppPanel(SchemaBrowserApplicationModel applicationModel) {
		super(applicationModel, createPanels(applicationModel), emptyList());
	}

	private static List<EntityPanel> createPanels(SchemaBrowserApplicationModel applicationModel) {
		SwingEntityModel schemaModel = applicationModel.entityModels().get(Schema.TYPE);
		SwingEntityModel tableModel = schemaModel.detailModels().get(Table.TYPE);
		SwingEntityModel columnModel = tableModel.detailModels().get(TableColumn.TYPE);
		SwingEntityModel constraintModel = tableModel.detailModels().get(Constraint.TYPE);
		SwingEntityModel columnConstraintModel = constraintModel.detailModels().get(ConstraintColumn.TYPE);

		EntityPanel schemaPanel = new EntityPanel(schemaModel,
						config -> config.detailLayout(entityPanel -> TabbedDetailLayout.builder()
										.panel(entityPanel)
										.splitPaneResizeWeight(0.3)
										.build()));
		EntityPanel tablePanel = new EntityPanel(tableModel,
						config -> config.detailLayout(entityPanel -> WindowDetailLayout.builder()
										.panel(entityPanel)
										.build()));
		EntityPanel columnPanel = new EntityPanel(columnModel);
		EntityPanel constraintPanel = new EntityPanel(constraintModel);
		EntityPanel columnConstraintPanel = new EntityPanel(columnConstraintModel);

		schemaPanel.detailPanels().add(tablePanel);
		tablePanel.detailPanels().add(columnPanel);
		tablePanel.detailPanels().add(constraintPanel);
		constraintPanel.detailPanels().add(columnConstraintPanel);

		schemaModel.tableModel().items().refresh();

		return List.of(schemaPanel);
	}

	public static void main(String[] args) {
		FilterTable.AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
		EntityTablePanel.Config.CONDITION_VIEW.set(ConditionView.SIMPLE);
		EntityTablePanel.Config.POPUP_MENU_LAYOUT.set(Controls.layout(asList(
						ControlKeys.REFRESH,
						null,
						ControlKeys.ADDITIONAL_POPUP_MENU_CONTROLS,
						null,
						ControlKeys.CONDITION_CONTROLS,
						null,
						ControlKeys.COPY_CONTROLS
		)));
		EntityApplication.builder(SchemaBrowserApplicationModel.class, SchemaBrowserAppPanel.class)
						.domain(SchemaBrowser.DOMAIN)
						.name("Schema Browser")
						.defaultUser(User.parse("scott:tiger"))
						.start();
	}

	public static final class SchemaBrowserApplicationModel extends SwingEntityApplicationModel {
		public SchemaBrowserApplicationModel(EntityConnectionProvider connectionProvider) {
			super(connectionProvider, List.of(createSchemaModel(connectionProvider)));
		}

		private static SwingEntityModel createSchemaModel(EntityConnectionProvider connectionProvider) {
			SwingEntityModel schemaModel = new SwingEntityModel(Schema.TYPE, connectionProvider);
			SwingEntityModel tableModel = new SwingEntityModel(Table.TYPE, connectionProvider);
			SwingEntityModel columnModel = new SwingEntityModel(TableColumn.TYPE, connectionProvider);
			SwingEntityModel constraintModel = new SwingEntityModel(Constraint.TYPE, connectionProvider);
			SwingEntityModel constraintColumnModel = new SwingEntityModel(ConstraintColumn.TYPE, connectionProvider);

			schemaModel.detailModels().add(tableModel);
			tableModel.detailModels().add(columnModel, constraintModel);
			constraintModel.detailModels().add(constraintColumnModel);

			return schemaModel;
		}
	}
}