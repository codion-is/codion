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
 * Copyright (c) 2010 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.tools.randomizer;

import is.codion.common.event.Event;
import is.codion.common.model.randomizer.ItemRandomizer;
import is.codion.common.value.AbstractValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.layout.Layouts;

import javax.swing.DefaultListModel;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingConstants;
import java.awt.BorderLayout;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Consumer;

import static java.util.Objects.requireNonNull;

/**
 * A default UI for the ItemRandomizer class.
 * For instances use the factory method {@link #itemRandomizerPanel(ItemRandomizer)}.
 * @param <T> the type of items being randomized
 */
public final class ItemRandomizerPanel<T> extends JPanel {

	private static final int SPINNER_COLUMNS = 3;

	private final ItemRandomizer<T> itemRandomizer;
	private final JPanel configPanel = new JPanel(Layouts.gridLayout(0, 1));
	private final JList<ItemRandomizer.RandomItem<T>> itemList = new JList<>(new DefaultListModel<>());
	private final Event<List<ItemRandomizer.RandomItem<T>>> selectedItemChangedEvent = Event.event();

	private ItemRandomizerPanel(ItemRandomizer<T> itemRandomizer) {
		this.itemRandomizer = requireNonNull(itemRandomizer, "itemRandomizer");
		initializeUI();
	}

	/**
	 * @return the randomizer this panel is based on
	 */
	public ItemRandomizer<T> itemRandomizer() {
		return itemRandomizer;
	}

	/**
	 * @param listener a listener notified each time the selected item changes
	 */
	public void addSelectedItemListener(Consumer<List<ItemRandomizer.RandomItem<T>>> listener) {
		selectedItemChangedEvent.addDataListener(listener);
	}

	/**
	 * @param listener the listener to remove
	 */
	public void removeSelectedItemListener(Consumer<List<ItemRandomizer.RandomItem<T>>> listener) {
		selectedItemChangedEvent.removeDataListener(listener);
	}

	/**
	 * @return the currently selected item
	 */
	public List<ItemRandomizer.RandomItem<T>> selectedItems() {
		return itemList.getSelectedValuesList();
	}

	/**
	 * Instantiates a new {@link ItemRandomizerPanel}.
	 * @param itemRandomizer the item randomizer
	 * @param <T> the item type
	 * @return a new {@link ItemRandomizerPanel}
	 */
	public static <T> ItemRandomizerPanel<T> itemRandomizerPanel(ItemRandomizer<T> itemRandomizer) {
		return new ItemRandomizerPanel<>(itemRandomizer);
	}

	private void initializeUI() {
		List<ItemRandomizer.RandomItem<T>> items = new ArrayList<>(itemRandomizer.items());
		items.sort(Comparator.comparing(item -> item.item().toString()));
		items.forEach(((DefaultListModel<ItemRandomizer.RandomItem<T>>) itemList.getModel())::addElement);
		itemList.addListSelectionListener(e -> selectedItemChangedEvent.accept(itemList.getSelectedValuesList()));
		addSelectedItemListener(selectedItems -> {
			configPanel.removeAll();
			for (ItemRandomizer.RandomItem<T> item : selectedItems) {
				configPanel.add(createWeightPanel(item));
			}
			revalidate();
		});
		setLayout(Layouts.borderLayout());
		add(new JScrollPane(itemList), BorderLayout.CENTER);
		add(configPanel, BorderLayout.SOUTH);
	}

	/**
	 * Returns a JPanel with controls configuring the weight of the given item
	 * @param item the item for which to create a configuration panel
	 * @return a control panel for the item weight
	 */
	private JPanel createWeightPanel(ItemRandomizer.RandomItem<T> item) {
		return Components.borderLayoutPanel(Layouts.borderLayout())
						.northComponent(new JLabel(item.item().toString()))
						.westComponent(Components.checkBox(new EnabledModelValue(item.item()))
										.text("Enabled")
										.build())
						.centerComponent(Components.label("Weight")
										.horizontalAlignment(SwingConstants.RIGHT)
										.build())
						.eastComponent(Components.integerSpinner(new WeightModelValue(item.item()))
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
		public Boolean get() {
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
		public Integer get() {
			return itemRandomizer.weight(item);
		}

		@Override
		protected void setValue(Integer value) {
			itemRandomizer.setWeight(item, value);
		}
	}
}