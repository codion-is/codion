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
package is.codion.swing.common.ui.component.panel;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.awt.BorderLayout;

import static is.codion.swing.common.ui.layout.Layouts.borderLayout;
import static java.util.Objects.requireNonNull;

final class DefaultInputPanelBorderLayout implements InputPanelLayout {

	private final String labelConstraints;
	private final String componentConstraints;

	private DefaultInputPanelBorderLayout(DefaultBuilder builder) {
		this.labelConstraints = builder.labelConstraints;
		this.componentConstraints = builder.componentConstraints;
	}

	@Override
	public JPanel layout(JComponent label, JComponent component) {
		JPanel inputPanel = new JPanel(borderLayout());
		inputPanel.add(label, labelConstraints);
		inputPanel.add(component, componentConstraints);

		return inputPanel;
	}

	static class DefaultBuilder implements InputPanelBorderLayoutBuilder {

		private String labelConstraints = BorderLayout.NORTH;
		private String componentConstraints =  BorderLayout.CENTER;

		@Override
		public InputPanelBorderLayoutBuilder labelConstraints(String constraints) {
			if (requireNonNull(constraints).equals(componentConstraints)) {
				throw new IllegalArgumentException("labelConstraints must differ from componentConstraints");
			}
			this.labelConstraints = requireNonNull(constraints);
			return this;
		}

		@Override
		public InputPanelBorderLayoutBuilder componentConstraints(String constraints) {
			if (requireNonNull(constraints).equals(labelConstraints)) {
				throw new IllegalArgumentException("componentConstraints must differ from labelConstraints");
			}
			this.componentConstraints = requireNonNull(constraints);
			return this;
		}

		@Override
		public InputPanelLayout build() {
			return new DefaultInputPanelBorderLayout(this);
		}
	}
}
