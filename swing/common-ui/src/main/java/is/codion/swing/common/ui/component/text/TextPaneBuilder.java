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

import is.codion.common.value.Value;

import javax.swing.JTextArea;
import javax.swing.JTextPane;
import javax.swing.text.Document;
import javax.swing.text.StyledDocument;

import static java.util.Objects.requireNonNull;

/**
 * Builds a JTextPane.
 */
public interface TextPaneBuilder extends TextComponentBuilder<String, JTextPane, TextPaneBuilder> {

	/**
	 * @param autoscrolls true if autoscrolling should be enabled
	 * @return this builder instance
	 * @see JTextPane#setAutoscrolls(boolean)
	 */
	TextPaneBuilder autoscrolls(boolean autoscrolls);

	/**
	 * @param document the document
	 * @return this builder instance
	 * @see JTextArea#setDocument(Document)
	 */
	TextPaneBuilder document(StyledDocument document);

	/**
	 * @return a builder for a component
	 */
	static TextPaneBuilder builder() {
		return new DefaultTextPaneBuilder(null);
	}

	/**
	 * @param linkedValue the value to link to the component
	 * @return a builder for a component
	 */
	static TextPaneBuilder builder(Value<String> linkedValue) {
		return new DefaultTextPaneBuilder(requireNonNull(linkedValue));
	}
}
