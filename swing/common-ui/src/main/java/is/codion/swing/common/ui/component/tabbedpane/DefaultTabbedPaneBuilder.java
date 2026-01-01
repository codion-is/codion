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
package is.codion.swing.common.ui.component.tabbedpane;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeListener;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

import static java.util.Objects.requireNonNull;

final class DefaultTabbedPaneBuilder extends AbstractComponentBuilder<JTabbedPane, TabbedPaneBuilder> implements TabbedPaneBuilder {

	private int tabPlacement = SwingConstants.TOP;
	private int tabLayoutPolicy = JTabbedPane.WRAP_TAB_LAYOUT;
	private final List<DefaultTabBuilder> tabBuilders = new ArrayList<>();
	private final List<ChangeListener> changeListeners = new ArrayList<>();

	@Override
	public TabbedPaneBuilder tabPlacement(int tabPlacement) {
		this.tabPlacement = tabPlacement;
		return this;
	}

	@Override
	public TabbedPaneBuilder tabLayoutPolicy(int tabLayoutPolicy) {
		this.tabLayoutPolicy = tabLayoutPolicy;
		return this;
	}

	@Override
	public TabbedPaneBuilder changeListener(ChangeListener changeListener) {
		changeListeners.add(requireNonNull(changeListener));
		return this;
	}

	@Override
	public TabbedPaneBuilder tab(String title, JComponent component) {
		new DefaultTabBuilder(this, title)
						.component(component)
						.add();
		return this;
	}

	@Override
	public TabbedPaneBuilder tab(String title, Supplier<? extends JComponent> component) {
		return tab(title, requireNonNull(component).get());
	}

	@Override
	public TabBuilder tab(String title) {
		return new DefaultTabBuilder(this, title);
	}

	@Override
	protected JTabbedPane createComponent() {
		JTabbedPane tabbedPane = new JTabbedPane(tabPlacement, tabLayoutPolicy);
		tabBuilders.forEach(tabBuilder -> {
			int tabIndex = tabbedPane.getTabCount();
			tabbedPane.addTab(tabBuilder.title, tabBuilder.icon, tabBuilder.component, tabBuilder.toolTipText);
			if (tabBuilder.mnemonic != 0) {
				tabbedPane.setMnemonicAt(tabIndex, tabBuilder.mnemonic);
			}
			if (tabBuilder.tabComponent != null) {
				tabbedPane.setTabComponentAt(tabIndex, tabBuilder.tabComponent);
			}
		});
		changeListeners.forEach(new AddChangeListener(tabbedPane));

		return tabbedPane;
	}

	private static final class DefaultTabBuilder implements TabBuilder {

		private final DefaultTabbedPaneBuilder tabbedPaneBuilder;
		private final String title;

		private @Nullable JComponent component;
		private int mnemonic;
		private @Nullable String toolTipText;
		private @Nullable Icon icon;
		private @Nullable JComponent tabComponent;

		private DefaultTabBuilder(DefaultTabbedPaneBuilder tabbedPaneBuilder, String title) {
			this.tabbedPaneBuilder = tabbedPaneBuilder;
			this.title = requireNonNull(title);
		}

		@Override
		public TabBuilder component(@Nullable JComponent component) {
			this.component = component;
			return this;
		}

		@Override
		public TabBuilder component(Supplier<? extends JComponent> component) {
			return component(requireNonNull(component).get());
		}

		@Override
		public TabBuilder mnemonic(int mnemonic) {
			this.mnemonic = mnemonic;
			return this;
		}

		@Override
		public TabBuilder toolTipText(@Nullable String toolTipText) {
			this.toolTipText = toolTipText;
			return this;
		}

		@Override
		public TabBuilder icon(@Nullable Icon icon) {
			this.icon = icon;
			return this;
		}

		@Override
		public TabBuilder tabComponent(@Nullable JComponent tabComponent) {
			this.tabComponent = tabComponent;
			return this;
		}

		@Override
		public TabbedPaneBuilder add() {
			tabbedPaneBuilder.tabBuilders.add(this);

			return tabbedPaneBuilder;
		}
	}

	private static final class AddChangeListener implements Consumer<ChangeListener> {

		private final JTabbedPane tabbedPane;

		private AddChangeListener(JTabbedPane tabbedPane) {
			this.tabbedPane = tabbedPane;
		}

		@Override
		public void accept(ChangeListener listener) {
			tabbedPane.addChangeListener(listener);
		}
	}
}
