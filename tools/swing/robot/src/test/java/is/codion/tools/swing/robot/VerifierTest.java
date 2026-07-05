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
package is.codion.tools.swing.robot;

import is.codion.tools.swing.robot.Interaction.Delivery;

import org.junit.jupiter.api.Test;

import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JTextField;
import java.awt.event.KeyEvent;

import static java.awt.event.InputEvent.CTRL_DOWN_MASK;
import static java.awt.event.InputEvent.SHIFT_DOWN_MASK;
import static java.awt.event.KeyEvent.*;
import static javax.swing.KeyStroke.getKeyStroke;
import static org.junit.jupiter.api.Assertions.*;

public final class VerifierTest {

	@Test
	void consumed() {
		Verifier verifier = new Verifier();
		JTextField field = new JTextField();
		field.setName("quantity");
		KeyEvent event = press(field, CTRL_DOWN_MASK, VK_S);
		event.consume();
		Interaction interaction = verifier.verify(getKeyStroke("ctrl S"), () -> verifier.postProcessed(event), 500);
		assertEquals(Delivery.CONSUMED, interaction.delivery());
		assertEquals("JTextField[quantity]", interaction.component());
		assertEquals("ctrl S", interaction.keyStroke());
	}

	@Test
	void fellThrough() {
		Verifier verifier = new Verifier();
		JTextField field = new JTextField();
		KeyEvent event = press(field, CTRL_DOWN_MASK, VK_S);// not consumed
		Interaction interaction = verifier.verify(getKeyStroke("ctrl S"), () -> verifier.postProcessed(event), 500);
		assertEquals(Delivery.FELL_THROUGH, interaction.delivery());
		assertEquals("JTextField", interaction.component());
	}

	@Test
	void missed() {
		Verifier verifier = new Verifier();
		Interaction interaction = verifier.verify(getKeyStroke("ctrl S"), () -> {}, 100);
		assertEquals(Delivery.MISSED, interaction.delivery());
		assertNull(interaction.component());
		assertNull(interaction.action());
	}

	@Test
	void dispatchedButEaten() {
		Verifier verifier = new Verifier();
		JTextField field = new JTextField();
		KeyEvent event = press(field, CTRL_DOWN_MASK, VK_S);
		// seen entering dispatch but consumed by another dispatcher, never post-processed
		Interaction interaction = verifier.verify(getKeyStroke("ctrl S"), () -> verifier.dispatched(event), 100);
		assertEquals(Delivery.CONSUMED, interaction.delivery());
	}

	@Test
	void typedCharacter() {
		Verifier verifier = new Verifier();
		JTextField field = new JTextField();
		KeyEvent event = new KeyEvent(field, KEY_TYPED, 1L, 0, VK_UNDEFINED, 'a');
		Interaction interaction = verifier.verify(getKeyStroke('a'), () -> verifier.postProcessed(event), 500);
		assertEquals(Delivery.FELL_THROUGH, interaction.delivery());
		assertEquals("typed a", interaction.keyStroke());
	}

	@Test
	void typedUppercase() {
		Verifier verifier = new Verifier();
		JTextField field = new JTextField();
		// an uppercase character is typed with shift down, so the KEY_TYPED carries the shift modifier
		KeyEvent event = new KeyEvent(field, KEY_TYPED, 1L, SHIFT_DOWN_MASK, VK_UNDEFINED, 'T');
		Interaction interaction = verifier.verify(getKeyStroke('T'), () -> verifier.postProcessed(event), 500);
		assertEquals(Delivery.FELL_THROUGH, interaction.delivery());// matched despite the shift modifier, not MISSED
	}

	@Test
	void actionResolved() {
		Verifier verifier = new Verifier();
		JButton button = new JButton();
		button.getInputMap(JComponent.WHEN_FOCUSED).put(getKeyStroke("ctrl S"), "save");
		KeyEvent event = press(button, CTRL_DOWN_MASK, VK_S);
		event.consume();
		Interaction interaction = verifier.verify(getKeyStroke("ctrl S"), () -> verifier.postProcessed(event), 500);
		assertEquals(Delivery.CONSUMED, interaction.delivery());
		assertEquals("save", interaction.action());
	}

	@Test
	void matches() {
		JTextField field = new JTextField();
		assertTrue(Verifier.matches(getKeyStroke("ctrl S"), press(field, CTRL_DOWN_MASK, VK_S)));
		assertTrue(Verifier.matches(getKeyStroke("ENTER"), press(field, 0, VK_ENTER)));
		// a modifier key press is not the significant event
		assertFalse(Verifier.matches(getKeyStroke("ctrl S"), press(field, CTRL_DOWN_MASK, VK_CONTROL)));
		// a key release is not the significant event
		assertFalse(Verifier.matches(getKeyStroke("ctrl S"),
						new KeyEvent(field, KEY_RELEASED, 1L, CTRL_DOWN_MASK, VK_S, CHAR_UNDEFINED)));
	}

	private static KeyEvent press(JComponent source, int modifiers, int keyCode) {
		return new KeyEvent(source, KEY_PRESSED, 1L, modifiers, keyCode, CHAR_UNDEFINED);
	}
}
