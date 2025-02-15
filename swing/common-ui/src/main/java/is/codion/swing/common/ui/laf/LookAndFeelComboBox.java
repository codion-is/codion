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
 * Copyright (c) 2022 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.laf;

import is.codion.common.item.Item;
import is.codion.common.property.PropertyValue;
import is.codion.swing.common.model.component.combobox.FilterComboBoxModel;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static is.codion.common.Configuration.booleanValue;
import static is.codion.common.item.Item.item;
import static is.codion.swing.common.ui.component.combobox.ComboBoxBuilder.enableMouseWheelSelection;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeels;
import static java.util.stream.Collectors.toList;
import static javax.swing.UIManager.getLookAndFeel;

/**
 * A combo box for selecting a LookAndFeel.
 * Instantiate via factory methods {@link #lookAndFeelComboBox()} or {@link #lookAndFeelComboBox(boolean)}.
 * @see #lookAndFeelComboBox()
 * @see #lookAndFeelComboBox(boolean)
 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelInfo)
 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelEnabler)
 */
public final class LookAndFeelComboBox extends JComboBox<Item<LookAndFeelEnabler>> {

	/**
	 * Specifies whether to enable the Look and Feel dynamically when selected
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: true
	 * </ul>
	 */
	public static final PropertyValue<Boolean> ENABLE_ON_SELECTION =
					booleanValue(LookAndFeelComboBox.class.getName() + ".enableOnSelection", true);

	private final LookAndFeelEnabler originalLookAndFeel = createOriginalLookAndFeel();

	private LookAndFeelComboBox(boolean enableOnSelection) {
		super(createLookAndFeelComboBoxModel());
		Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults = new ConcurrentHashMap<>();
		setRenderer(new LookAndFeelRenderer(lookAndFeelDefaults));
		setEditor(new LookAndFeelEditor(lookAndFeelDefaults));
		enableMouseWheelSelection(this);
		getModel().selection().item().set(item(originalLookAndFeel));
		if (enableOnSelection) {
			getModel().selection().item().addConsumer(lookAndFeelProvider ->
							SwingUtilities.invokeLater(() -> lookAndFeelProvider.value().enable()));
		}
	}

	@Override
	public FilterComboBoxModel<Item<LookAndFeelEnabler>> getModel() {
		return (FilterComboBoxModel<Item<LookAndFeelEnabler>>) super.getModel();
	}

	/**
	 * @return the currently selected look and feel
	 */
	public LookAndFeelEnabler selectedLookAndFeel() {
		return getModel().selection().item().getOrThrow().value();
	}

	/**
	 * Enables the currently selected look and feel, if it is already enabled, this method does nothing
	 */
	public void enableSelected() {
		String currentLookAndFeelClassName = getLookAndFeel().getClass().getName();
		if (!selectedLookAndFeel().lookAndFeelInfo().getClassName().equals(currentLookAndFeelClassName)) {
			selectedLookAndFeel().enable();
		}
	}

	/**
	 * Reverts the look and feel to the look and feel active when this look and feel combobox was created,
	 * if it is already enabled, this method does nothing
	 */
	public void revert() {
		String currentLookAndFeelClassName = getLookAndFeel().getClass().getName();
		if (originalLookAndFeel != null && !currentLookAndFeelClassName.equals(originalLookAndFeel.lookAndFeelInfo().getClassName())) {
			originalLookAndFeel.enable();
		}
	}

	/**
	 * Instantiates a new {@link LookAndFeelComboBox} displaying the available look and feels
	 * @return a new {@link LookAndFeelComboBox} instance
	 */
	public static LookAndFeelComboBox lookAndFeelComboBox() {
		return new LookAndFeelComboBox(ENABLE_ON_SELECTION.getOrThrow());
	}

	/**
	 * Instantiates a new {@link LookAndFeelComboBox} displaying the available look and feels
	 * @param enableOnSelection if true the look and feel is enabled dynamically when selected
	 * @return a new {@link LookAndFeelComboBox} instance
	 */
	public static LookAndFeelComboBox lookAndFeelComboBox(boolean enableOnSelection) {
		return new LookAndFeelComboBox(enableOnSelection);
	}

	private static final class LookAndFeelEditor extends BasicComboBoxEditor {

		private final LookAndFeelPanel panel;

		private Item<LookAndFeelEnabler> item;

		private LookAndFeelEditor(Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults) {
			panel = new LookAndFeelPanel(lookAndFeelDefaults);
		}

		@Override
		public Component getEditorComponent() {
			return panel;
		}

		@Override
		public Object getItem() {
			return item;
		}

		@Override
		public void setItem(Object item) {
			this.item = (Item<LookAndFeelEnabler>) item;
			if (this.item != null) {
				panel.setLookAndFeel(this.item.value(), false);
			}
		}
	}

	private static final class LookAndFeelRenderer implements ListCellRenderer<Item<LookAndFeelEnabler>> {

		private final LookAndFeelPanel panel;

		private LookAndFeelRenderer(Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults) {
			panel = new LookAndFeelPanel(lookAndFeelDefaults);
		}

		@Override
		public Component getListCellRendererComponent(JList<? extends Item<LookAndFeelEnabler>> list, Item<LookAndFeelEnabler> value,
																									int index, boolean isSelected, boolean cellHasFocus) {
			if (value != null) {
				panel.setLookAndFeel(value.value(), isSelected);
			}

			return panel;
		}
	}

	private static LookAndFeelEnabler createOriginalLookAndFeel() {
		LookAndFeel lookAndFeel = getLookAndFeel();

		return new DefaultLookAndFeelEnabler(new LookAndFeelInfo(lookAndFeel.getName(), lookAndFeel.getClass().getName()));
	}

	private static FilterComboBoxModel<Item<LookAndFeelEnabler>> createLookAndFeelComboBoxModel() {
		return FilterComboBoxModel.builder(lookAndFeels().stream()
										.map(provider -> item(provider, provider.lookAndFeelInfo().getName()))
										.collect(toList()))
						.sorted(true)
						.build();
	}
}
