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
 * Copyright (c) 2010 - 2025, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui;

import is.codion.common.reactive.observer.Observer;
import is.codion.common.reactive.state.State;

import org.junit.jupiter.api.Test;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JComponent;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class UtilitiesTest {

	@Test
	void enabled() {
		Action action = new AbstractAction("test") {
			@Override
			public void actionPerformed(ActionEvent e) {}
		};
		State state = State.state();

		try {
			Utilities.enabled(state, action);
			assertFalse(action.isEnabled());
			state.set(true);
			Thread.sleep(50);//due to EDT
			assertTrue(action.isEnabled());
			state.set(false);
			Thread.sleep(50);//due to EDT
			assertFalse(action.isEnabled());
		}
		catch (InterruptedException ignored) {/*ignored*/}

		JComponent comp = new JTextField();
		state = State.state();

		State theState = state;
		try {
			Utilities.enabled(theState, comp);
			assertFalse(comp.isEnabled());
			theState.set(true);
			Thread.sleep(50);//due to EDT
			assertTrue(comp.isEnabled());
			theState.set(false);
			Thread.sleep(50);//due to EDT
			assertFalse(comp.isEnabled());
		}
		catch (InterruptedException e) {/*ignored*/}
	}

	@Test
	void observer() {
		JTextField textField = new JTextField();
		AtomicInteger counter = new AtomicInteger();
		Observer<Integer> alignmentObserver =
						Utilities.observer(textField, "horizontalAlignment");
		alignmentObserver.addListener(counter::incrementAndGet);
		textField.setHorizontalAlignment(SwingConstants.TRAILING);
		assertEquals(1, counter.get());
		textField.setHorizontalAlignment(SwingConstants.LEADING);
		assertEquals(2, counter.get());
	}
}
