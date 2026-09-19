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
import is.codion.common.reactive.state.State;
import is.codion.swing.common.ui.component.indicator.ValidationIndicator;

import com.formdev.flatlaf.FlatClientProperties;
import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.SwingUtilities;

import static com.formdev.flatlaf.FlatClientProperties.*;
import static java.util.Objects.requireNonNull;

/**
 * <p>A FlatLaf based {@link ValidationIndicator} implementation, using the FlatLaf 'outline' client property.
 * <p>FlatLaf carries a value for each of the two severities, {@link FlatClientProperties#OUTLINE_ERROR} and
 * {@link FlatClientProperties#OUTLINE_WARNING}, along with {@link FlatClientProperties#OUTLINE_SUCCESS}.
 * <p>A success is not the absence of an error, an empty optional field being valid without being a success,
 * it is a state of its own, a repeated password matching or a code having been verified. It is thereby specified
 * per component, by way of an instance based on the success state: {@code validationIndicator(new FlatValidationIndicator(success))}
 */
public final class FlatValidationIndicator implements ValidationIndicator {

	private final ObservableState success;

	/**
	 * Instantiates a new {@link FlatValidationIndicator}, one never indicating success
	 */
	public FlatValidationIndicator() {
		// a never-successful state stands in, rather than the indicator having to cope with its absence
		this(State.state().observable());
	}

	/**
	 * Instantiates a new {@link FlatValidationIndicator}, indicating success while the given state is enabled,
	 * unless the value is invalid or warned, those taking precedence. Note that the success state applies to each
	 * component this indicator is enabled for, such an instance is meant for a single component.
	 * @param success the success state
	 * @see FlatClientProperties#OUTLINE_SUCCESS
	 */
	public FlatValidationIndicator(ObservableState success) {
		this.success = requireNonNull(success);
	}

	@Override
	public void enable(JComponent component, ObservableState invalid, ObservableState warned) {
		new Indicator(requireNonNull(component), requireNonNull(invalid), requireNonNull(warned), success);
	}

	private static final class Indicator {

		private final JComponent component;
		private final ObservableState invalid;
		private final ObservableState warned;
		private final ObservableState success;

		private Indicator(JComponent component, ObservableState invalid, ObservableState warned, ObservableState success) {
			this.component = component;
			this.invalid = invalid;
			this.warned = warned;
			this.success = success;
			invalid.addListener(this::update);
			warned.addListener(this::update);
			success.addListener(this::update);
			update();
		}

		// Invalid wins over warned: you act on what blocks you first, and the warning is still there once you have.
		// Both win over success.
		private void update() {
			String outline = outline();
			SwingUtilities.invokeLater(() -> component.putClientProperty(OUTLINE, outline));
		}

		private @Nullable String outline() {
			if (invalid.is()) {
				return OUTLINE_ERROR;
			}
			if (warned.is()) {
				return OUTLINE_WARNING;
			}

			return success.is() ? OUTLINE_SUCCESS : null;
		}
	}
}
