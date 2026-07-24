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
 * Copyright (c) 2022 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.laf;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.reactive.state.State;
import is.codion.common.utilities.item.Item;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.model.component.combobox.SwingFilterComboBoxModel;

import org.jspecify.annotations.Nullable;

import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.basic.BasicComboBoxEditor;
import java.awt.Component;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import java.util.function.Predicate;

import static is.codion.common.utilities.Configuration.booleanValue;
import static is.codion.common.utilities.item.Item.item;
import static is.codion.swing.common.ui.Utilities.enabled;
import static is.codion.swing.common.ui.component.combobox.ComboBoxBuilder.enableMouseWheelSelection;
import static is.codion.swing.common.ui.laf.LookAndFeelProvider.lookAndFeels;
import static java.util.stream.Collectors.toList;
import static javax.swing.UIManager.getLookAndFeel;

/**
 * A combo box for selecting a LookAndFeel.
 * Instantiate via builder {@link #builder()}.
 * @see #builder()
 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelInfo)
 * @see LookAndFeelProvider#addLookAndFeel(LookAndFeelEnabler)
 */
public final class LookAndFeelComboBox extends JComboBox<Item<LookAndFeelEnabler>> {

	/**
	 * <p>Specifies whether to include the platform look and feels in the selection combo box by default.
	 * <ul>
	 * <li>Value type: Boolean
	 * <li>Default value: false
	 * </ul>
	 * @see UIManager#getInstalledLookAndFeels()
	 * @see is.codion.swing.common.ui.laf.LookAndFeelProvider
	 */
	public static final PropertyValue<Boolean> PLATFORM =
					booleanValue(LookAndFeelComboBox.class.getName() + ".platform", false);

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

	private final State platform;
	private final State light;
	private final State dark;

	private LookAndFeelComboBox(DefaultBuilder builder) {
		super(createLookAndFeelComboBoxModel());
		Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults = new ConcurrentHashMap<>();
		setRenderer(new LookAndFeelRenderer(lookAndFeelDefaults));
		setEditor(new LookAndFeelEditor(lookAndFeelDefaults));
		enableMouseWheelSelection(this);
		getModel().selection().item().set(item(originalLookAndFeel));
		if (builder.enabled != null) {
			enabled(builder.enabled, this);
		}
		platform = State.builder()
						.value(builder.platform)
						.listener(getModel().items()::filter)
						.build();
		light = State.builder()
						.value(true)
						.listener(getModel().items()::filter)
						.build();
		dark = State.builder()
						.value(true)
						.listener(getModel().items()::filter)
						.build();
		getModel().items().included().predicate()
						.set(new IncludePredicate());
		if (builder.onSelection != null) {
			getModel().selection().item().addConsumer(item ->
							builder.onSelection.accept(item.getOrThrow()));
		}
		if (builder.enableOnSelection) {
			getModel().selection().item().addConsumer(lookAndFeelProvider ->
							SwingUtilities.invokeLater(() -> lookAndFeelProvider.getOrThrow().enable()));
		}
	}

	@Override
	public SwingFilterComboBoxModel<Item<LookAndFeelEnabler>> getModel() {
		return (SwingFilterComboBoxModel<Item<LookAndFeelEnabler>>) super.getModel();
	}

	/**
	 * @return a {@link State} controlling whether the platform look and feels are included
	 */
	public State platform() {
		return platform;
	}

	/**
	 * @return a {@link State} controlling whether light look and feels are included
	 */
	public State light() {
		return light;
	}

	/**
	 * @return a {@link State} controlling whether dark look and feels are included
	 */
	public State dark() {
		return dark;
	}

	/**
	 * @return the currently selected look and feel
	 */
	public LookAndFeelEnabler selectedLookAndFeel() {
		return getModel().selection().item().getOrThrow().getOrThrow();
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
	 * Instantiates a new {@link LookAndFeelComboBox.Builder} displaying the available look and feels
	 * @return a new {@link LookAndFeelComboBox.Builder} instance
	 */
	public static LookAndFeelComboBox.Builder builder() {
		return new LookAndFeelComboBox.DefaultBuilder();
	}

	/**
	 * Builds a {@link LookAndFeelComboBox}
	 */
	public interface Builder {

		/**
		 * @param enableOnSelection true if look and feel should be enabled when selected
		 * @return this builder
		 */
		Builder enableOnSelection(boolean enableOnSelection);

		/**
		 * @param platform true if installed platform look and feels should be included
		 * @return this builder
		 */
		Builder platform(boolean platform);

		/**
		 * @param enabled the enabled observer
		 * @return this builder
		 */
		Builder enabled(@Nullable ObservableState enabled);

		/**
		 * @param onSelection called when the selection changes
		 * @return this builcer
		 */
		Builder onSelection(@Nullable Consumer<LookAndFeelEnabler> onSelection);

		/**
		 * @return a new {@link LookAndFeelComboBox}
		 */
		LookAndFeelComboBox build();
	}

	private static final class DefaultBuilder implements Builder {

		private @Nullable ObservableState enabled;
		private boolean platform = PLATFORM.getOrThrow();
		private boolean enableOnSelection = ENABLE_ON_SELECTION.getOrThrow();
		private @Nullable Consumer<LookAndFeelEnabler> onSelection;

		@Override
		public Builder enableOnSelection(boolean enableOnSelection) {
			this.enableOnSelection = enableOnSelection;
			return this;
		}

		@Override
		public Builder platform(boolean platform) {
			this.platform = platform;
			return this;
		}

		@Override
		public Builder enabled(@Nullable ObservableState enabled) {
			this.enabled = enabled;
			return this;
		}

		@Override
		public Builder onSelection(@Nullable Consumer<LookAndFeelEnabler> onSelection) {
			this.onSelection = onSelection;
			return this;
		}

		@Override
		public LookAndFeelComboBox build() {
			return new LookAndFeelComboBox(this);
		}
	}

	private static final class LookAndFeelEditor extends BasicComboBoxEditor {

		private final LookAndFeelPanel panel;

		private @Nullable Item<LookAndFeelEnabler> item;

		private LookAndFeelEditor(Map<LookAndFeelEnabler, Map<String, Object>> lookAndFeelDefaults) {
			panel = new LookAndFeelPanel(lookAndFeelDefaults);
		}

		@Override
		public Component getEditorComponent() {
			return panel;
		}

		@Override
		public @Nullable Object getItem() {
			return item;
		}

		@Override
		public void setItem(@Nullable Object item) {
			this.item = (Item<LookAndFeelEnabler>) item;
			if (this.item != null) {
				panel.setLookAndFeel(this.item.getOrThrow(), false);
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
				panel.setLookAndFeel(value.getOrThrow(), isSelected);
			}

			return panel;
		}
	}

	private class IncludePredicate implements Predicate<Item<LookAndFeelEnabler>> {

		@Override
		public boolean test(Item<LookAndFeelEnabler> item) {
			LookAndFeelEnabler enabler = item.getOrThrow();
			if (!light.is() && !enabler.dark()) {
				return false;
			}
			if (!dark.is() && enabler.dark()) {
				return false;
			}

			return platform.is() || !enabler.platform();
		}
	}

	private static LookAndFeelEnabler createOriginalLookAndFeel() {
		LookAndFeel lookAndFeel = getLookAndFeel();

		return new DefaultLookAndFeelEnabler(new LookAndFeelInfo(lookAndFeel.getName(), lookAndFeel.getClass().getName()));
	}

	private static SwingFilterComboBoxModel<Item<LookAndFeelEnabler>> createLookAndFeelComboBoxModel() {
		return SwingFilterComboBoxModel.builder()
						.items(lookAndFeels().stream()
										.map(provider -> item(provider, provider.lookAndFeelInfo().getName()))
										.collect(toList()))
						.sorted(true)
						.build();
	}
}
