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
package is.codion.swing.common.ui.component.text;

import is.codion.swing.common.ui.component.value.ComponentValue;

import org.jspecify.annotations.Nullable;

import javax.swing.JTextPane;
import javax.swing.text.StyledDocument;

import static java.util.Objects.requireNonNull;

final class DefaultTextPaneBuilder extends AbstractTextComponentBuilder<JTextPane, String, TextPaneBuilder>
				implements TextPaneBuilder {

	private boolean autoscrolls = false;
	private @Nullable StyledDocument document;

	DefaultTextPaneBuilder() {}

	@Override
	public TextPaneBuilder autoscrolls(boolean autoscrolls) {
		this.autoscrolls = autoscrolls;
		return this;
	}

	@Override
	public TextPaneBuilder document(StyledDocument document) {
		this.document = requireNonNull(document);
		return this;
	}

	@Override
	protected JTextPane createTextComponent() {
		JTextPane textPane = new JTextPane();
		if (document != null) {
			textPane.setStyledDocument(document);
		}
		textPane.setAutoscrolls(autoscrolls);

		return textPane;
	}

	@Override
	protected ComponentValue<JTextPane, String> createValue(JTextPane component) {
		return new DefaultTextComponentValue<>(component, null, updateOn());
	}
}
