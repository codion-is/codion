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
package is.codion.swing.common.ui.component.indicator;

import is.codion.common.reactive.state.ObservableState;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import java.awt.Color;

import static is.codion.swing.common.ui.color.Colors.darker;
import static java.util.Objects.requireNonNull;

public final class BackgroundColorValidIndicator implements ValidIndicator {

	@Override
	public void enable(JComponent component, ObservableState valid) {
		new Indicator(requireNonNull(component), requireNonNull(valid));
	}

	private static final class Indicator {

		private final JComponent component;
		private final String uiComponentKey;

		private @Nullable Color backgroundColor;
		private @Nullable Color inactiveBackgroundColor;
		private @Nullable Color invalidBackgroundColor;

		private Indicator(JComponent component, ObservableState valid) {
			requireNonNull(valid);
			this.component = requireNonNull(component);
			this.uiComponentKey = initializeUiComponentKey();
			if (componentSupported(uiComponentKey)) {
				component.addPropertyChangeListener("UI", event -> configureColors(valid.is()));
				valid.addConsumer(this::update);
				update(valid.is());
			}
		}

		private void update(boolean valid) {
			boolean enabled = component.isEnabled();
			SwingUtilities.invokeLater(() -> {
				if (valid) {
					component.setBackground(enabled ? backgroundColor : inactiveBackgroundColor);
				}
				else {
					component.setBackground(invalidBackgroundColor);
				}
			});
		}

		private void configureColors(boolean valid) {
			this.backgroundColor = UIManager.getColor(uiComponentKey + ".background");
			this.inactiveBackgroundColor = UIManager.getColor(uiComponentKey + ".inactiveBackground");
			this.invalidBackgroundColor = darker(backgroundColor);
			update(valid);
		}

		private String initializeUiComponentKey() {
			String uiClassID = component.getUIClassID();
			//remove "UI" suffix
			return uiClassID.substring(0, uiClassID.length() - 2);
		}

		private static boolean componentSupported(String uiComponentKey) {
			return UIManager.getColor(uiComponentKey + ".background") != null &&
							UIManager.getColor(uiComponentKey + ".inactiveBackground") != null;
		}
	}
}
