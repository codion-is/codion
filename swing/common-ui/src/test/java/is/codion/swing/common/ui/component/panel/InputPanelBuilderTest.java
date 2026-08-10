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

import org.junit.jupiter.api.Test;

import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public final class InputPanelBuilderTest {

	@Test
	void labelAndComponentRequired() {
		JTextField field = new JTextField();
		assertThrows(IllegalStateException.class, () -> InputPanelBuilder.builder().component(field).build());
		assertThrows(IllegalStateException.class, () -> InputPanelBuilder.builder().label("Label").build());
		assertNotNull(InputPanelBuilder.builder().label("Label").component(field).build());
	}

	@Test
	void everyLabelOverloadSpecifiesTheSameLabel() {
		// The panel contains its label, so the two InputPanelBuilder overloads and the three inherited from
		// ComponentBuilder all mean the same thing - they differ only in what they accept. Which one a call
		// binds to is decided by the argument's static type, invisibly at the call site, so any of them must
		// be able to replace any other: the last call wins, as with any other builder value.
		JLabel first = new JLabel("First");
		JTextField second = new JTextField("Second");

		assertSame(second, labelOf(builder().label(first).label((JComponent) second)));
		assertSame(first, labelOf(builder().label((JComponent) second).label(first)));
		assertSame(second, labelOf(builder().label("Text").label(() -> second)));
		assertSame(first, labelOf(builder().label(() -> second).label(first)));
		assertEquals("Text", ((JLabel) labelOf(builder().label((JComponent) second).label("Text"))).getText());
		assertEquals("Built", ((JLabel) labelOf(builder()
						.label((JComponent) second)
						.label(label -> label.text("Built")))).getText());
	}

	@Test
	void aFactorySuppliedLabelIsReplaceable() {
		// How EditorComponents.inputPanel(attribute) hands over a caption based label: as a JComponent, since
		// it need not be a JLabel. Overriding it used to depend on which overload the override happened to
		// bind to - label(String) wrote a different field than label(JComponent) and lost to it.
		JComponent factoryLabel = new JLabel("Caption");

		assertEquals("Overridden", ((JLabel) labelOf(builder()
						.label(factoryLabel)
						.label("Overridden"))).getText());
		JTextField ownLabel = new JTextField("Own");
		assertSame(ownLabel, labelOf(builder()
						.label(factoryLabel)
						.label(() -> ownLabel)));
	}

	@Test
	void aLabelWithNoAssociationIsAssociatedWithTheInputComponent() {
		// A label built from text alone has no labelFor, so its mnemonic would have nothing to focus. The
		// panel is never the target - it is the input component the label labels.
		JTextField field = new JTextField();
		JPanel panel = InputPanelBuilder.builder().label("Label").component(field).build();

		JLabel label = (JLabel) labelOf(panel);
		assertSame(field, label.getLabelFor());
		assertNotSame(panel, label.getLabelFor());
	}

	@Test
	void anExplicitAssociationStands() {
		// The attribute components come with a label already associated with them; building an input panel
		// around one must not re-target it.
		JTextField labelled = new JTextField();
		JLabel label = new JLabel("Label");
		label.setLabelFor(labelled);

		InputPanelBuilder.builder().label(label).component(new JTextField()).build();
		assertSame(labelled, label.getLabelFor());
	}

	@Test
	void aNonLabelLabelIsLeftAlone() {
		JTextField field = new JTextField();
		JTextField label = new JTextField("Label");
		JPanel panel = InputPanelBuilder.builder().label((JComponent) label).component(field).build();

		assertSame(label, labelOf(panel));
	}

	private static InputPanelBuilder builder() {
		return InputPanelBuilder.builder().component(new JTextField());
	}

	private static JComponent labelOf(InputPanelBuilder builder) {
		return labelOf(builder.build());
	}

	// The label is whichever child is not the input component; the layout decides where it goes.
	private static JComponent labelOf(JPanel panel) {
		List<JComponent> children = Arrays.stream(panel.getComponents())
						.map(JComponent.class::cast)
						.toList();
		assertEquals(2, children.size());

		return children.get(0);
	}
}
