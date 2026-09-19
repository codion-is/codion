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
package is.codion.plugin.flatlaf.indicator;

import is.codion.common.reactive.state.ObservableState;
import is.codion.swing.common.ui.component.indicator.ValidationIndicator;

import com.formdev.flatlaf.FlatClientProperties;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import static java.util.Objects.requireNonNull;

/**
 * <p>A FlatLaf based {@link ValidationIndicator} implementation, using the FlatLaf 'outline' client property.
 * <p>FlatLaf carries a value for each of the two severities — {@link FlatClientProperties#OUTLINE_ERROR} and
 * {@link FlatClientProperties#OUTLINE_WARNING}.
 */
public final class FlatLafValidationIndicator implements ValidationIndicator {

	@Override
	public void enable(JComponent component, ObservableState invalid, ObservableState warned) {
		new Indicator(requireNonNull(component), requireNonNull(invalid), requireNonNull(warned));
	}

	private static final class Indicator {

		private final JComponent component;
		private final ObservableState invalid;
		private final ObservableState warned;

		private Indicator(JComponent component, ObservableState invalid, ObservableState warned) {
			this.component = component;
			this.invalid = invalid;
			this.warned = warned;
			invalid.addListener(this::update);
			warned.addListener(this::update);
			update();
		}

		// Invalid wins over warned: you act on what blocks you first, and the warning is still there once you have.
		private void update() {
			String outline = outline();
			SwingUtilities.invokeLater(() -> component.putClientProperty(FlatClientProperties.OUTLINE, outline));
		}

		private @Nullable String outline() {
			if (invalid.is()) {
				return FlatClientProperties.OUTLINE_ERROR;
			}

			return warned.is() ? FlatClientProperties.OUTLINE_WARNING : null;
		}
	}
}
