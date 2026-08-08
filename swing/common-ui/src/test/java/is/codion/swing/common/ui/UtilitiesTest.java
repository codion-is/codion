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
 * Copyright (c) 2010 - 2026, Björn Darri Sigurðsson.
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
import javax.swing.SwingUtilities;
import java.awt.event.ActionEvent;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

public class UtilitiesTest {

	@Test
	void enabled() throws Exception {
		Action action = new AbstractAction("test") {
			@Override
			public void actionPerformed(ActionEvent e) {}
		};
		State actionState = State.state();

		Utilities.enabled(actionState, action);
		assertFalse(action.isEnabled());
		actionState.set(true);
		flushEventQueue();
		assertTrue(action.isEnabled());
		actionState.set(false);
		flushEventQueue();
		assertFalse(action.isEnabled());

		JComponent component = new JTextField();
		State componentState = State.state();

		Utilities.enabled(componentState, component);
		assertFalse(component.isEnabled());
		componentState.set(true);
		flushEventQueue();
		assertTrue(component.isEnabled());
		componentState.set(false);
		flushEventQueue();
		assertFalse(component.isEnabled());
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

	/**
	 * Returns once the Event Dispatch Thread has processed everything queued before this call.
	 * {@link Utilities#enabled} applies the change via invokeLater when called off the EDT, as it
	 * is here, so the state change is not visible on return from {@link State#set(Object)}.
	 * Since invokeLater is FIFO, this is exact where a sleep is merely probable.
	 */
	private static void flushEventQueue() throws Exception {
		SwingUtilities.invokeAndWait(() -> {});
	}
}
