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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.tools.loadtest.ui;

import is.codion.common.observable.Observable;
import is.codion.common.value.AbstractValue;
import is.codion.swing.common.model.component.list.FilterListModel;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.list.FilterList;
import is.codion.swing.common.ui.component.value.ComponentValue;
import is.codion.tools.loadtest.randomizer.ItemRandomizer;
import is.codion.tools.loadtest.randomizer.ItemRandomizer.RandomItem;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.List;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static is.codion.swing.common.ui.layout.Layouts.gridLayout;
import static java.util.Objects.requireNonNull;

/**
 * A default UI for the ItemRandomizer class.
 * @param <T> the type of items being randomized
 */
final class ItemRandomizerPanel<T> extends JPanel {

	private static final int SPINNER_COLUMNS = 3;

	private final ItemRandomizer<T> itemRandomizer;
	private final JPanel configPanel = new JPanel(gridLayout(0, 1));
	private final ComponentValue<List<RandomItem<T>>, FilterList<RandomItem<T>>> listValue;

	ItemRandomizerPanel(ItemRandomizer<T> itemRandomizer) {
		this.itemRandomizer = requireNonNull(itemRandomizer);
		this.listValue = Components.list()
						.model(FilterListModel.builder()
										.items(itemRandomizer.items())
										.build())
						.selectedItems()
						.buildValue();
		initializeUI();
	}

	/**
	 * @return the randomizer this panel is based on
	 */
	public ItemRandomizer<T> itemRandomizer() {
		return itemRandomizer;
	}

	/**
	 * @return the selected items
	 */
	public Observable<List<RandomItem<T>>> selectedItems() {
		return listValue.component().model().selection().items().observable();
	}

	private void initializeUI() {
		listValue.addConsumer(selected -> {
			configPanel.removeAll();
			selected.forEach(item -> configPanel.add(createWeightPanel(item)));
			revalidate();
		});
		setLayout(borderLayout());
		add(new JScrollPane(listValue.component()), BorderLayout.CENTER);
		add(configPanel, BorderLayout.SOUTH);
	}

	/**
	 * Returns a JPanel with controls configuring the weight of the given item
	 * @param item the item for which to create a configuration panel
	 * @return a control panel for the item weight
	 */
	private JPanel createWeightPanel(RandomItem<T> item) {
		return Components.borderLayoutPanel()
						.layout(borderLayout())
						.northComponent(new JLabel(item.item().toString()))
						.westComponent(Components.checkBox()
										.link(new EnabledModelValue(item.item()))
										.text("Enabled")
										.build())
						.centerComponent(Components.label()
										.text("Weight")
										.horizontalAlignment(SwingConstants.RIGHT)
										.build())
						.eastComponent(Components.integerSpinner()
										.link(new WeightModelValue(item.item()))
										.minimum(0)
										.columns(SPINNER_COLUMNS)
										.toolTipText(item.item().toString())
										.build())
						.build();
	}

	private final class EnabledModelValue extends AbstractValue<Boolean> {

		private final T item;

		private EnabledModelValue(T item) {
			super(false);
			this.item = item;
		}

		@Override
		protected Boolean getValue() {
			return itemRandomizer.isItemEnabled(item);
		}

		@Override
		protected void setValue(Boolean value) {
			itemRandomizer.setItemEnabled(item, value);
		}
	}

	private final class WeightModelValue extends AbstractValue<Integer> {

		private final T item;

		private WeightModelValue(T item) {
			super(0);
			this.item = item;
		}

		@Override
		protected Integer getValue() {
			return itemRandomizer.weight(item);
		}

		@Override
		protected void setValue(Integer value) {
			itemRandomizer.setWeight(item, value);
		}
	}
}