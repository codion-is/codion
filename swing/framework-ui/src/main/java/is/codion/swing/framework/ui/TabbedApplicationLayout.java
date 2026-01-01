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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.layout.Layouts;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import java.awt.BorderLayout;

import static is.codion.common.utilities.Configuration.integerValue;
import static is.codion.swing.common.ui.component.Components.borderLayoutPanel;
import static java.util.Objects.requireNonNull;
import static javax.swing.BorderFactory.createEmptyBorder;

/**
 * A {@link is.codion.swing.framework.ui.EntityApplicationPanel.ApplicationLayout} layout based on a {@link JTabbedPane}.
 */
public class TabbedApplicationLayout implements EntityApplicationPanel.ApplicationLayout {

	/**
	 * Specifies the tab placement
	 * <ul>
	 * <li>Value type: Integer (SwingConstants.TOP, SwingConstants.BOTTOM, SwingConstants.LEFT, SwingConstants.RIGHT)
	 * <li>Default value: {@link SwingConstants#TOP}
	 * </ul>
	 */
	public static final PropertyValue<Integer> TAB_PLACEMENT =
					integerValue(TabbedApplicationLayout.class.getName() + ".tabPlacement", SwingConstants.TOP);

	private final EntityApplicationPanel<?> applicationPanel;

	private @Nullable JTabbedPane tabbedPane;

	/**
	 * @param applicationPanel the application panel to layout
	 */
	public TabbedApplicationLayout(EntityApplicationPanel<?> applicationPanel) {
		this.applicationPanel = requireNonNull(applicationPanel);
	}

	/**
	 * @return a {@link javax.swing.JPanel} using a {@link BorderLayout} containing a {@link JTabbedPane} with a tab for each root entity panel.
	 * @see EntityApplicationPanel#entityPanels()
	 */
	@Override
	public JComponent layout() {
		if (tabbedPane != null) {
			throw new IllegalStateException("EntityApplicationPanel has already been laid out: " + applicationPanel);
		}
		tabbedPane = Components.tabbedPane()
						.tabPlacement(TAB_PLACEMENT.getOrThrow())
						.focusable(false)
						// InitializeSelectedPanelListener initializes first panel
						.changeListener(new InitializeSelectedPanelListener())
						.build();

		applicationPanel.entityPanels().forEach(this::addTab);

		//tab pane added to a base panel for correct Look&Feel rendering
		return borderLayoutPanel()
						.layout(new BorderLayout())
						.center(tabbedPane)
						.border(createEmptyBorder(0, Layouts.GAP.getOrThrow(), 0, Layouts.GAP.getOrThrow()))
						.build();
	}

	@Override
	public final void activated(EntityPanel entityPanel) {
		requireNonNull(entityPanel);
		if (tabbedPane == null) {
			throw new IllegalStateException("EntityApplicationPanel has not been laid out");
		}
		if (tabbedPane.indexOfComponent(entityPanel) != -1) {
			entityPanel.initialize();
			tabbedPane.setSelectedComponent(entityPanel);
		}
	}

	/**
	 * @return the application tabbed pane
	 * @throws IllegalStateException in case the application panel has not been laid out
	 */
	public final JTabbedPane tabbedPane() {
		if (tabbedPane == null) {
			throw new IllegalStateException("EntityApplicationPanel has not been laid out: " + applicationPanel);
		}

		return tabbedPane;
	}

	private void addTab(EntityPanel entityPanel) {
		tabbedPane.addTab(entityPanel.caption(), entityPanel);
		tabbedPane.setToolTipTextAt(tabbedPane.getTabCount() - 1, entityPanel.description().orElse(null));
		tabbedPane.setIconAt(tabbedPane.getTabCount() - 1, entityPanel.icon().orElse(null));
	}

	private final class InitializeSelectedPanelListener implements ChangeListener {

		@Override
		public void stateChanged(ChangeEvent e) {
			if (tabbedPane.getTabCount() > 0) {
				((EntityPanel) tabbedPane.getSelectedComponent()).activate();
			}
		}
	}
}
