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
package is.codion.swing.common.ui.component.panel;

import java.awt.FlowLayout;

import static is.codion.swing.common.ui.layout.Layouts.flowLayout;

final class DefaultFlowLayoutPanelBuilder extends DefaultPanelBuilder<FlowLayout, FlowLayoutPanelBuilder>
				implements FlowLayoutPanelBuilder {

	DefaultFlowLayoutPanelBuilder(int align) {
		this(flowLayout(align));
	}

	DefaultFlowLayoutPanelBuilder(FlowLayout layout) {
		layout(layout);
	}
}
