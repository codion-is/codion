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

import is.codion.swing.common.ui.component.Components;
import is.codion.swing.common.ui.component.text.TextFieldBuilder;
import is.codion.swing.common.ui.component.text.TextInput;

import org.junit.jupiter.api.Test;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.ComponentOrientation;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;

import static is.codion.swing.common.ui.layout.Layouts.GAP;
import static javax.swing.SwingConstants.CENTER;
import static javax.swing.SwingConstants.TRAILING;
import static org.junit.jupiter.api.Assertions.*;

public final class FormBuilderTest {

	@Test
	void pairsWrapAtColumns() {
		JTextField first = field("First");
		JTextField second = field("Second");
		JTextField third = field("Third");
		JPanel form = Components.form()
						.columns(2)
						.add(first)
						.add(second)
						.add(third)
						.build();
		assertGrid(form, labelOf(first), 0, 0, 1);
		assertGrid(form, first, 1, 0, 1);
		assertGrid(form, labelOf(second), 2, 0, 1);
		assertGrid(form, second, 3, 0, 1);
		assertGrid(form, labelOf(third), 0, 1, 1);
		assertGrid(form, third, 1, 1, 1);
		// the labels are recovered from the components
		assertTrue(form.isAncestorOf(labelOf(first)));
	}

	@Test
	void labelColumnWidthAndSlack() {
		JTextField shortLabelled = field("A");
		JTextField longLabelled = field("A much longer label");
		JPanel form = Components.form()
						.add(shortLabelled)
						.add(longLabelled)
						.build();
		form.setSize(400, 100);
		form.doLayout();
		int widestLabel = labelOf(longLabelled).getPreferredSize().width;
		assertEquals(0, labelOf(shortLabelled).getX());
		assertEquals(0, labelOf(longLabelled).getX());
		assertEquals(widestLabel + GAP.getOrThrow(), shortLabelled.getX());
		assertEquals(shortLabelled.getX(), longLabelled.getX());
		assertEquals(400, shortLabelled.getX() + shortLabelled.getWidth());
		assertEquals(400, longLabelled.getX() + longLabelled.getWidth());
	}

	@Test
	void trailingLabels() {
		JTextField shortLabelled = field("A");
		JTextField longLabelled = field("A much longer label");
		JPanel form = Components.form()
						.labelAlignment(TRAILING)
						.add(shortLabelled)
						.add(longLabelled)
						.build();
		form.setSize(400, 100);
		form.doLayout();
		JLabel shortLabel = labelOf(shortLabelled);
		JLabel longLabel = labelOf(longLabelled);
		assertEquals(longLabel.getX() + longLabel.getWidth(), shortLabel.getX() + shortLabel.getWidth());
		assertTrue(shortLabel.getX() > longLabel.getX());
	}

	@Test
	void span() {
		JTextField first = field("First");
		JTextField wide = field("Wide");
		JPanel bare = new JPanel();
		JTextField last = field("Last");
		JPanel form = Components.form()
						.columns(2)
						.add(first)
						.span(wide)
						.span(bare)
						.add(last)
						.build();
		// a span starts a new row after a partial one, a labelled one keeping the label column
		assertGrid(form, labelOf(wide), 0, 1, 1);
		assertGrid(form, wide, 1, 1, 3);
		// a label-less one spans the full width
		assertGrid(form, bare, 0, 2, 4);
		assertGrid(form, labelOf(last), 0, 3, 1);
		form.setSize(400, 200);
		form.doLayout();
		assertEquals(400, wide.getX() + wide.getWidth());
		assertEquals(0, bare.getX());
		assertEquals(400, bare.getWidth());
	}

	@Test
	void labelLessComponentInTheInputColumn() {
		JCheckBox checkBox = new JCheckBox("Remember");
		JTextField field = field("Name");
		JPanel form = Components.form()
						.add(field)
						.add(checkBox)
						.build();
		assertGrid(form, checkBox, 1, 1, 1);
		form.setSize(300, 100);
		form.doLayout();
		assertEquals(field.getX(), checkBox.getX());
	}

	@Test
	void labelAtTheTopOfATallInput() {
		JPanel tall = new JPanel();
		tall.setPreferredSize(new Dimension(100, 80));
		JLabel label = new JLabel("Cover");
		JPanel form = Components.form()
						.add(label, tall)
						.build();
		form.setSize(300, 200);
		form.doLayout();
		assertEquals(tall.getY(), label.getY());
		assertEquals(80, tall.getHeight());
	}

	@Test
	void inputsStretchToTheRowUnlessTheirBaselineWouldMove() {
		// a tall panel on the left of each row sets the row height
		JPanel tall = tallPanel();
		JPanel shortPanel = new JPanel();
		shortPanel.setPreferredSize(new Dimension(100, 60));
		JLabel shortLabel = new JLabel("Cover");
		JScrollPane textArea = new JScrollPane(new JTextArea(3, 10));
		JTextField field = field("Name");
		TextInput textInput = Components.textInput().build();
		JLabel textLabel = new JLabel("Text");
		JPanel form = Components.form()
						.columns(2)
						.add(new JLabel("Tags"), tall)
						.add(shortLabel, shortPanel)
						.add(new JLabel("Tags"), tallPanel())
						.add(new JLabel("Notes"), textArea)
						.add(new JLabel("Tags"), tallPanel())
						.add(field)
						.add(new JLabel("Tags"), tallPanel())
						.add(textLabel, textInput)
						.build();
		form.setSize(500, 800);
		form.doLayout();
		// a panel and a text area stretch to the row
		assertEquals(tall.getHeight(), shortPanel.getHeight());
		assertTrue(textArea.getHeight() > textArea.getPreferredSize().height + 50);
		// single line inputs keep their height, the composite text input reporting the baseline of its field
		assertEquals(field.getPreferredSize().height, field.getHeight());
		assertEquals(textInput.getPreferredSize().height, textInput.getHeight());
		// the label at the top of the stretched panel, on the baseline of the single line inputs
		assertEquals(shortPanel.getY(), shortLabel.getY());
		assertTrue(Math.abs(labelOf(field).getY() - field.getY()) <= 3);
		assertTrue(Math.abs(textLabel.getY() - textInput.getY()) <= 3);
	}

	@Test
	void formKeepsToTheTop() {
		JTextField field = field("Name");
		JPanel form = Components.form()
						.add(field)
						.build();
		form.setSize(300, 300);
		form.doLayout();
		assertEquals(0, field.getY());
	}

	@Test
	void labelHiddenWithItsInput() throws Exception {
		JTextField field = field("Name");
		JLabel label = labelOf(field);
		Components.form()
						.add(field)
						.build();
		// the component shown/hidden events arrive via the event queue
		field.setVisible(false);
		SwingUtilities.invokeAndWait(() -> {});
		assertFalse(label.isVisible());
		field.setVisible(true);
		SwingUtilities.invokeAndWait(() -> {});
		assertTrue(label.isVisible());
		// the initial state applies as well
		JTextField hidden = field("Hidden");
		hidden.setVisible(false);
		Components.form()
						.add(hidden)
						.build();
		assertFalse(labelOf(hidden).isVisible());
	}

	@Test
	void rightToLeft() {
		JTextField field = field("Name");
		JPanel form = Components.form()
						.add(field)
						.build();
		form.applyComponentOrientation(ComponentOrientation.RIGHT_TO_LEFT);
		form.setSize(300, 100);
		form.doLayout();
		assertTrue(labelOf(field).getX() > field.getX());
	}

	@Test
	void labelFor() {
		JTextField field = new JTextField();
		JLabel label = new JLabel("Name");
		Components.form()
						.add(label, field)
						.build();
		assertSame(field, label.getLabelFor());
		// an explicit association stands
		JTextField other = new JTextField();
		JLabel otherLabel = new JLabel("Other");
		otherLabel.setLabelFor(other);
		Components.form()
						.add(otherLabel, field)
						.build();
		assertSame(other, otherLabel.getLabelFor());
	}

	@Test
	void validation() {
		assertThrows(IllegalArgumentException.class, () -> Components.form().columns(0));
		assertThrows(IllegalArgumentException.class, () -> Components.form().labelAlignment(CENTER));
		assertThrows(NullPointerException.class, () -> Components.form().add((JComponent) null));
	}

	private static JPanel tallPanel() {
		JPanel panel = new JPanel();
		panel.setPreferredSize(new Dimension(100, 150));

		return panel;
	}

	private static JTextField field(String label) {
		return TextFieldBuilder.builder()
						.valueClass(String.class)
						.label(label)
						.build();
	}

	private static JLabel labelOf(JComponent component) {
		return (JLabel) component.getClientProperty("labeledBy");
	}

	private static void assertGrid(JPanel form, JComponent component, int gridx, int gridy, int gridwidth) {
		GridBagConstraints constraints = ((GridBagLayout) form.getLayout()).getConstraints(component);
		assertEquals(gridx, constraints.gridx, "gridx");
		assertEquals(gridy, constraints.gridy, "gridy");
		assertEquals(gridwidth, constraints.gridwidth, "gridwidth");
	}
}
