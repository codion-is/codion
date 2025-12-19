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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.demos.chinook.ui;

import is.codion.common.model.condition.ConditionModel;
import is.codion.common.reactive.value.Value;
import is.codion.demos.chinook.domain.api.Chinook.Preferences;
import is.codion.framework.domain.entity.EntityDefinition;
import is.codion.swing.common.ui.component.table.ColumnConditionPanel.ConditionComponents;
import is.codion.swing.common.ui.component.table.FilterTableCellEditor;
import is.codion.swing.framework.model.SwingEntityTableModel;
import is.codion.swing.framework.ui.EntityConditionComponents;
import is.codion.swing.framework.ui.EntityTablePanel;

import com.formdev.flatlaf.extras.components.FlatTriStateCheckBox;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.table.TableCellRenderer;
import java.awt.Component;

public final class PreferencesTablePanel extends EntityTablePanel {

	public PreferencesTablePanel(SwingEntityTableModel tableModel) {
		super(tableModel, config -> config
						.editComponent(Preferences.NEWSLETTER_SUBSCRIBED,
										e -> new TriStateCheckBoxValue())
						.surrendersFocusOnKeystroke(true)
						.cellRenderer(Preferences.NEWSLETTER_SUBSCRIBED, renderer -> renderer
										.renderer(new FlatTriStateRenderer()))
						.cellEditor(Preferences.NEWSLETTER_SUBSCRIBED, FilterTableCellEditor.builder()
										.component(TriStateCheckBoxValue::new)
										.clickCountToStart(1)
										.build())
						.filterComponents(Preferences.NEWSLETTER_SUBSCRIBED, new NewsletterSubscribedFilterComponents())
						.conditionComponents(Preferences.NEWSLETTER_SUBSCRIBED, new NewsletterSubscribedConditionComponents(tableModel.entityDefinition())));
	}

	private static final class FlatTriStateRenderer
					extends FlatTriStateCheckBox implements TableCellRenderer {

		private FlatTriStateRenderer() {
			setHorizontalAlignment(CENTER);
		}

		@Override
		public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected,
																									 boolean hasFocus, int row, int column) {
			setChecked((Boolean) value);

			return this;
		}
	}

	private static final class NewsletterSubscribedFilterComponents implements ConditionComponents {

		@Override
		public <T> JComponent equal(ConditionModel<T> conditionModel) {
			TriStateCheckBoxValue value = new TriStateCheckBoxValue();
			value.link((Value<Boolean>) conditionModel.operands().equal());

			return value.component();
		}
	}

	private static final class NewsletterSubscribedConditionComponents extends EntityConditionComponents {

		private NewsletterSubscribedConditionComponents(EntityDefinition entityDefinition) {
			super(entityDefinition);
		}

		@Override
		public <T> JComponent equal(ConditionModel<T> conditionModel) {
			TriStateCheckBoxValue value = new TriStateCheckBoxValue();
			value.link((Value<Boolean>) conditionModel.operands().equal());

			return value.component();
		}
	}
}
