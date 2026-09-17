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
 * Copyright (c) 2026, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.component.panel;

import is.codion.swing.common.ui.component.builder.AbstractComponentBuilder;

import org.jspecify.annotations.Nullable;

import javax.swing.Box;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import java.awt.Component.BaselineResizeBehavior;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

import static is.codion.swing.common.ui.layout.Layouts.GAP;
import static java.util.Objects.requireNonNull;
import static javax.swing.SwingConstants.LEADING;
import static javax.swing.SwingConstants.TRAILING;

/**
 * A default {@link FormBuilder} implementation, extendable in order to add further ways of adding inputs.
 * @param <B> the builder type
 */
public class DefaultFormBuilder<B extends FormBuilder<B>> extends AbstractComponentBuilder<JPanel, B> implements FormBuilder<B> {

	// The client property JLabel.setLabelFor() puts on the component it labels
	private static final String LABELED_BY = "labeledBy";

	private final List<Entry> entries = new ArrayList<>();

	private int columns = 1;
	private int labelAlignment = LEADING;

	/**
	 * Instantiates a new {@link DefaultFormBuilder}
	 */
	protected DefaultFormBuilder() {}

	@Override
	public final B columns(int columns) {
		if (columns < 1) {
			throw new IllegalArgumentException("columns must be at least 1");
		}
		this.columns = columns;
		return self();
	}

	@Override
	public final B labelAlignment(int labelAlignment) {
		if (labelAlignment != LEADING && labelAlignment != TRAILING) {
			throw new IllegalArgumentException("labelAlignment must be LEADING or TRAILING");
		}
		this.labelAlignment = labelAlignment;
		return self();
	}

	@Override
	public final B add(JComponent component) {
		return add(labelOf(component), component, false);
	}

	@Override
	public final B add(Supplier<? extends JComponent> component) {
		return add(requireNonNull(component).get());
	}

	@Override
	public final B add(JComponent label, JComponent component) {
		return add(requireNonNull(label), component, false);
	}

	@Override
	public final B add(Supplier<? extends JComponent> label, Supplier<? extends JComponent> component) {
		return add(requireNonNull(label).get(), requireNonNull(component).get());
	}

	@Override
	public final B span(JComponent component) {
		return add(labelOf(component), component, true);
	}

	@Override
	public final B span(Supplier<? extends JComponent> component) {
		return span(requireNonNull(component).get());
	}

	@Override
	protected final JPanel createComponent() {
		JPanel panel = new JPanel(new GridBagLayout());
		int gap = GAP.getOrThrow();
		int gridColumns = columns * 2;
		int row = 0;
		int pair = 0;
		for (Entry entry : entries) {
			if (entry.span) {
				if (pair > 0) {
					row++;
					pair = 0;
				}
				if (entry.label == null) {
					panel.add(entry.component, componentConstraints(entry.component, 0, row, gridColumns, 0, 0, gap));
				}
				else {
					panel.add(entry.label, labelConstraints(0, row, 0, gap));
					panel.add(entry.component, componentConstraints(entry.component, 1, row, gridColumns - 1, gap, 0, gap));
				}
				row++;
			}
			else {
				int column = pair * 2;
				if (entry.label != null) {
					panel.add(entry.label, labelConstraints(column, row, pair > 0 ? gap : 0, gap));
				}
				panel.add(entry.component, componentConstraints(entry.component, column + 1, row, 1, gap, pair < columns - 1 ? gap : 0, gap));
				pair++;
				if (pair == columns) {
					row++;
					pair = 0;
				}
			}
			entry.labelFor();
			entry.mirrorVisibility();
		}
		panel.add(Box.createGlue(), fillerConstraints(pair > 0 ? row + 1 : row, gridColumns));

		return panel;
	}

	private B add(@Nullable JComponent label, JComponent component, boolean span) {
		entries.add(new Entry(label, requireNonNull(component), span));
		return self();
	}

	private GridBagConstraints labelConstraints(int gridx, int gridy, int left, int gap) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = gridx;
		constraints.gridy = gridy;
		constraints.anchor = labelAlignment == LEADING ? GridBagConstraints.BASELINE_LEADING : GridBagConstraints.BASELINE_TRAILING;
		constraints.insets = new Insets(gridy > 0 ? gap : 0, left, 0, 0);

		return constraints;
	}

	// A component without a baseline, such as a panel, is anchored to the top of its row, where GridBagLayout
	// would otherwise center it vertically, leaving it detached from its label when the row is taller than it is
	private static GridBagConstraints componentConstraints(JComponent component, int gridx, int gridy, int gridwidth, int left, int right, int gap) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = gridx;
		constraints.gridy = gridy;
		constraints.gridwidth = gridwidth;
		constraints.anchor = hasBaseline(component) ? GridBagConstraints.BASELINE_LEADING : GridBagConstraints.FIRST_LINE_START;
		constraints.fill = stretches(component) ? GridBagConstraints.BOTH : GridBagConstraints.HORIZONTAL;
		constraints.weightx = 1;
		constraints.insets = new Insets(gridy > 0 ? gap : 0, left, 0, right);

		return constraints;
	}

	// The extra height goes to an empty row below the last one, the form keeping to the top,
	// where GridBagLayout would otherwise center the rows vertically
	private static GridBagConstraints fillerConstraints(int gridy, int gridwidth) {
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = gridy;
		constraints.gridwidth = gridwidth;
		constraints.weighty = 1;

		return constraints;
	}

	private static boolean hasBaseline(JComponent component) {
		Dimension preferredSize = component.getPreferredSize();

		return component.getBaseline(preferredSize.width, preferredSize.height) >= 0;
	}

	// An input stretches to the height of its row, unless its baseline would move along with its height,
	// as with single line inputs, such as text fields, combo boxes and check boxes, which keep their height
	private static boolean stretches(JComponent component) {
		BaselineResizeBehavior behavior = component.getBaselineResizeBehavior();

		return behavior != BaselineResizeBehavior.CENTER_OFFSET && behavior != BaselineResizeBehavior.CONSTANT_DESCENT;
	}

	private static @Nullable JComponent labelOf(JComponent component) {
		return (JComponent) requireNonNull(component).getClientProperty(LABELED_BY);
	}

	private static final class Entry {

		private final @Nullable JComponent label;
		private final JComponent component;
		private final boolean span;

		private Entry(@Nullable JComponent label, JComponent component, boolean span) {
			this.label = label;
			this.component = component;
			this.span = span;
		}

		// A label built from text alone carries no labelFor, so its mnemonic would have nothing to focus,
		// an explicit association is the caller's and stands
		private void labelFor() {
			if (label instanceof JLabel && ((JLabel) label).getLabelFor() == null) {
				((JLabel) label).setLabelFor(component);
			}
		}

		// The component shown/hidden events arrive via the event queue, so the label follows a step behind
		private void mirrorVisibility() {
			if (label != null) {
				label.setVisible(component.isVisible());
				component.addComponentListener(new VisibilityMirror(label));
			}
		}
	}

	private static final class VisibilityMirror extends ComponentAdapter {

		private final JComponent label;

		private VisibilityMirror(JComponent label) {
			this.label = label;
		}

		@Override
		public void componentShown(ComponentEvent e) {
			label.setVisible(true);
		}

		@Override
		public void componentHidden(ComponentEvent e) {
			label.setVisible(false);
		}
	}
}
