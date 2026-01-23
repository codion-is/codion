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
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;

import static java.util.Objects.requireNonNull;

/**
 * Indicates modification by swapping the colors of the assocated label text.
 * Relies on {@link JLabel#setLabelFor(Component)}.
 * @see TextAttribute#SWAP_COLORS
 */
public final class SwapColorsModifiedIndicator implements ModifiedIndicator {

	@Override
	public void enable(JComponent component, ObservableState modified) {
		requireNonNull(modified).addConsumer(new SwapColorsIndicator(requireNonNull(component)));
	}

	private static final class SwapColorsIndicator extends AbstractModifiedIndicator {

		private SwapColorsIndicator(JComponent component) {
			super(component);
		}

		@Override
		protected void update(JLabel label, boolean modified) {
			Font font = label.getFont();
			Map<TextAttribute, @Nullable Object> attributes = (Map<TextAttribute, Object>) font.getAttributes();
			attributes.put(TextAttribute.SWAP_COLORS, modified ? TextAttribute.SWAP_COLORS_ON : null);
			label.setFont(font.deriveFont(attributes));
		}
	}
}
