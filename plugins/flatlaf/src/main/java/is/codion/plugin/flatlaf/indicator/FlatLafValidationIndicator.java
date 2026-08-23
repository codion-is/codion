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
 * {@link FlatClientProperties#OUTLINE_WARNING} — so both are drawn as the outline they should be, rather than
 * borrowing the background the way the look and feel agnostic fallback has to.
 */
public final class FlatLafValidationIndicator implements ValidationIndicator {

	@Override
	public void enable(JComponent component, ObservableState valid, ObservableState warned) {
		new Indicator(requireNonNull(component), requireNonNull(valid), requireNonNull(warned));
	}

	private static final class Indicator {

		private final JComponent component;
		private final ObservableState valid;
		private final ObservableState warned;

		private Indicator(JComponent component, ObservableState valid, ObservableState warned) {
			this.component = component;
			this.valid = valid;
			this.warned = warned;
			valid.addConsumer(state -> update());
			warned.addConsumer(state -> update());
			update();
		}

		// Invalid wins over warned: you act on what blocks you first, and the warning is still there once you have.
		private void update() {
			String outline = outline();
			SwingUtilities.invokeLater(() -> component.putClientProperty(FlatClientProperties.OUTLINE, outline));
		}

		private @Nullable String outline() {
			if (!valid.is()) {
				return FlatClientProperties.OUTLINE_ERROR;
			}

			return warned.is() ? FlatClientProperties.OUTLINE_WARNING : null;
		}
	}
}
