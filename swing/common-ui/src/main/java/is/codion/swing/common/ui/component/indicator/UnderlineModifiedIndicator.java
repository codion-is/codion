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
import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;

import static is.codion.common.utilities.Configuration.integerValue;
import static java.util.Objects.requireNonNull;

/**
 * Indicates modification by underlining the assocated label text.
 * Relies on {@link JLabel#setLabelFor(Component)}.
 * @see #UNDERLINE_STYLE
 */
public final class UnderlineModifiedIndicator implements ModifiedIndicator {

	/**
	 * The type of underline to use to indicate a modified value
	 * <ul>
	 * <li>Value type: Integer
	 * <li>Default value: {@link TextAttribute#UNDERLINE_LOW_DOTTED}
	 * <li>Valid values: {@link TextAttribute}.UNDERLINE_*
	 * </ul>
	 */
	public static final PropertyValue<Integer> UNDERLINE_STYLE =
					integerValue(ComponentBuilder.class.getName() + ".underlineStyle", TextAttribute.UNDERLINE_LOW_DOTTED);

	@Override
	public void enable(JComponent component, ObservableState modified) {
		requireNonNull(modified).addConsumer(new UnderlineIndicator(requireNonNull(component)));
	}

	private static final class UnderlineIndicator extends AbstractModifiedIndicator {

		private static final int UNDERLINE_STYLE = UnderlineModifiedIndicator.UNDERLINE_STYLE.getOrThrow();

		private UnderlineIndicator(JComponent component) {
			super(component);
		}

		@Override
		protected void update(JLabel label, boolean modified) {
			Font font = label.getFont();
			Map<TextAttribute, @Nullable Object> attributes = (Map<TextAttribute, Object>) font.getAttributes();
			attributes.put(TextAttribute.INPUT_METHOD_UNDERLINE, modified ? UNDERLINE_STYLE : null);
			label.setFont(font.deriveFont(attributes));
		}
	}
}
