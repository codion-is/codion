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
package is.codion.framework.demos.schemabrowser.client.ui;

import is.codion.common.user.User;
import is.codion.framework.db.EntityConnectionProvider;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Constraint;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.ConstraintColumn;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Schema;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.Table;
import is.codion.framework.demos.schemabrowser.domain.SchemaBrowser.TableColumn;
import is.codion.swing.common.ui.component.table.ConditionPanel.ConditionState;
import is.codion.swing.common.ui.component.table.FilterTable;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.framework.model.SwingEntityApplicationModel;
import is.codion.swing.framework.model.SwingEntityModel;
import is.codion.swing.framework.ui.EntityApplicationPanel;
import is.codion.swing.framework.ui.EntityPanel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.TabbedDetailLayout;
import is.codion.swing.framework.ui.WindowDetailLayout;

import com.formdev.flatlaf.intellijthemes.FlatAllIJThemes;

import javax.swing.JTable;
import java.util.Arrays;
import java.util.List;

public class SchemaBrowserAppPanel extends EntityApplicationPanel<SchemaBrowserAppPanel.SchemaBrowserApplicationModel> {

	private static final String DEFAULT_FLAT_LOOK_AND_FEEL = "com.formdev.flatlaf.intellijthemes.FlatArcIJTheme";

	public SchemaBrowserAppPanel(SchemaBrowserApplicationModel applicationModel) {
		super(applicationModel);
	}

	@Override
	protected List<EntityPanel> createEntityPanels() {
		SwingEntityModel schemaModel = applicationModel().entityModel(Schema.TYPE);
		SwingEntityModel tableModel = schemaModel.detailModel(Table.TYPE);
		SwingEntityModel columnModel = tableModel.detailModel(TableColumn.TYPE);
		SwingEntityModel constraintModel = tableModel.detailModel(Constraint.TYPE);
		SwingEntityModel columnConstraintModel = constraintModel.detailModel(ConstraintColumn.TYPE);

		EntityPanel schemaPanel = new EntityPanel(schemaModel,
						config -> config.detailLayout(entityPanel -> TabbedDetailLayout.builder(entityPanel)
										.splitPaneResizeWeight(0.3)
										.build()));
		EntityPanel tablePanel = new EntityPanel(tableModel,
						config -> config.detailLayout(entityPanel -> WindowDetailLayout.builder(entityPanel)
										.build()));
		EntityPanel columnPanel = new EntityPanel(columnModel);
		EntityPanel constraintPanel = new EntityPanel(constraintModel);
		EntityPanel columnConstraintPanel = new EntityPanel(columnConstraintModel);

		schemaPanel.addDetailPanel(tablePanel);
		tablePanel.addDetailPanels(columnPanel);
		tablePanel.addDetailPanel(constraintPanel);
		constraintPanel.addDetailPanel(columnConstraintPanel);

		schemaModel.tableModel().refresh();

		return List.of(schemaPanel);
	}

	public static void main(String[] args) {
		Arrays.stream(FlatAllIJThemes.INFOS)
						.forEach(LookAndFeelProvider::addLookAndFeel);
		FilterTable.AUTO_RESIZE_MODE.set(JTable.AUTO_RESIZE_ALL_COLUMNS);
		EntityTablePanel.Config.CONDITION_STATE.set(ConditionState.SIMPLE);
		EntityApplicationPanel.builder(SchemaBrowserApplicationModel.class, SchemaBrowserAppPanel.class)
						.applicationName("Schema Browser")
						.domainType(SchemaBrowser.DOMAIN)
						.defaultLoginUser(User.parse("scott:tiger"))
						.defaultLookAndFeelClassName(DEFAULT_FLAT_LOOK_AND_FEEL)
						.start();
	}

	public static final class SchemaBrowserApplicationModel extends SwingEntityApplicationModel {
		public SchemaBrowserApplicationModel(EntityConnectionProvider connectionProvider) {
			super(connectionProvider);
			SwingEntityModel schemaModel = new SwingEntityModel(Schema.TYPE, connectionProvider);
			SwingEntityModel tableModel = new SwingEntityModel(Table.TYPE, connectionProvider);
			SwingEntityModel columnModel = new SwingEntityModel(TableColumn.TYPE, connectionProvider);
			SwingEntityModel constraintModel = new SwingEntityModel(Constraint.TYPE, connectionProvider);
			SwingEntityModel constraintColumnModel = new SwingEntityModel(ConstraintColumn.TYPE, connectionProvider);

			schemaModel.addDetailModel(tableModel);
			tableModel.addDetailModels(columnModel, constraintModel);
			constraintModel.addDetailModels(constraintColumnModel);

			addEntityModel(schemaModel);
		}
	}
}