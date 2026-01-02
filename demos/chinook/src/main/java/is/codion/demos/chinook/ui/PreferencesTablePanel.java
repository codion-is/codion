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
 * Copyright (c) 2025 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.value.Value;
import is.codion.demos.chinook.domain.api.Chinook.Preferences;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionComponents;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.swing.framework.model.SwingEntityEditModel;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityTablePanel;
import is.codion.swing.framework.ui.component.EditComponent;

import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public final class PreferencesTablePanel extends EntityTablePanel {

	public PreferencesTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						.editComponent(Preferences.NEWSLETTER, new NewsletterEditor())
						.surrendersFocusOnKeystroke(true)
						.cellRenderer(Preferences.NEWSLETTER, renderer -> renderer
										.renderer(new NewsletterRenderer()))
						.cellEditor(Preferences.NEWSLETTER, FilterTableCellEditor.builder()
										.component(new TriStateCheckBoxBuilder()
														.altStateCycleOrder(true)
														::buildValue)
										.clickCountToStart(1)
										.resizeRow(false)
										.build())
						.filterComponents(Preferences.NEWSLETTER, new NewsletterConditionComponents())
						.conditionComponents(Preferences.NEWSLETTER, new NewsletterConditionComponents()));
	}

	private static final class NewsletterRenderer
					extends FlatTriStateCheckBox implements TableCellRenderer {

		private NewsletterRenderer() {
			setHorizontalAlignment(CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
																									 boolean hasFocus, int row, int column) {
			setChecked((Boolean) value);

			return this;
		}
	}

	private static final class NewsletterEditor implements EditComponent<FlatTriStateCheckBox, Boolean> {

		@Override
		public ComponentValue<FlatTriStateCheckBox, Boolean> component(SwingEntityEditModel editModel) {
			return new TriStateCheckBoxValue();
		}
	}

	private static final class NewsletterConditionComponents implements ConditionComponents {

		@Override
		public <T> JComponent equal(ConditionModel<T> conditionModel) {
			return new TriStateCheckBoxBuilder()
							.link(((Value<Boolean>) conditionModel.operands().equal()))
							.build();
		}
	}
}
