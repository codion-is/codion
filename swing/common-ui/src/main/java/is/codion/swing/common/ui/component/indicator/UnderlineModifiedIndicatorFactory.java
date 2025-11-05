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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.indicator;

import is.codion.common.reactive.state.ObservableState;
import is.codion.common.utilities.property.PropertyValue;
import is.codion.swing.common.ui.component.builder.ComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import java.awt.Component;
import java.awt.Font;
import java.awt.font.TextAttribute;
import java.util.Map;
import java.util.function.Consumer;

import static is.codion.common.utilities.Configuration.integerValue;
import static java.util.Objects.requireNonNull;

/**
 * Indicates modification by underlining the assocated label text.
 * Relies on {@link JLabel#setLabelFor(Component)}.
 */
public final class UnderlineModifiedIndicatorFactory implements ModifiedIndicatorFactory {

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
		requireNonNull(modified).addConsumer(new ModifiedIndicator(requireNonNull(component)));
	}

	private static final class ModifiedIndicator implements Consumer<Boolean> {

		private static final String LABELED_BY_PROPERTY = "labeledBy";
		private static final int UNDERLINE_STYLE = UnderlineModifiedIndicatorFactory.UNDERLINE_STYLE.getOrThrow();

		private final JComponent component;

		private ModifiedIndicator(JComponent component) {
			this.component = component;
		}

		@Override
		public void accept(Boolean modified) {
			JLabel label = (JLabel) component.getClientProperty(LABELED_BY_PROPERTY);
			if (label != null) {
				SwingUtilities.invokeLater(() -> setModifiedIndicator(label, modified));
			}
		}

		private static void setModifiedIndicator(JLabel label, boolean modified) {
			Font font = label.getFont();
			Map<TextAttribute, @Nullable Object> attributes = (Map<TextAttribute, Object>) font.getAttributes();
			attributes.put(TextAttribute.INPUT_METHOD_UNDERLINE, modified ? UNDERLINE_STYLE : null);
			label.setFont(font.deriveFont(attributes));
		}
	}
}
