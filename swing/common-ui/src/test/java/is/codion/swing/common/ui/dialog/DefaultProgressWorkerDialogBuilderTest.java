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
package is.codion.swing.common.ui.dialog;

import is.codion.swing.common.ui.dialog.DefaultProgressWorkerDialogBuilder.InputBlocker;

import org.junit.jupiter.api.Test;

import javax.swing.JFrame;
import javax.swing.JLayeredPane;
import javax.swing.JRootPane;
import javax.swing.JTextField;
import javax.swing.SwingUtilities;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.List;

import static java.util.Arrays.asList;
import static org.junit.jupiter.api.Assertions.*;

public final class DefaultProgressWorkerDialogBuilderTest {

	private static final int BLOCKING_LAYER = JLayeredPane.DRAG_LAYER + 1;

	@Test
	void inputBlocker() throws Exception {
		// input reached the owner of a modal progress dialog until the dialog showed, after SHOW_DELAY
		SwingUtilities.invokeAndWait(() -> {
			KeyboardFocusManager focusManager = KeyboardFocusManager.getCurrentKeyboardFocusManager();
			List<Character> reached = new ArrayList<>();
			// a key reaches a component only while it is showing, so the dispatchers after the blockers tell
			KeyEventDispatcher last = event -> {
				reached.add(event.getKeyChar());
				return false;
			};
			JTextField field = new JTextField();
			// never made visible
			JFrame frame = new JFrame();
			try {
				frame.add(field);
				JRootPane rootPane = frame.getRootPane();
				rootPane.setSize(200, 100);
				rootPane.doLayout();
				frame.getContentPane().doLayout();
				JLayeredPane layeredPane = frame.getLayeredPane();
				focusManager.addKeyEventDispatcher(last);
				type(field, 'a');
				assertSame(field, SwingUtilities.getDeepestComponentAt(rootPane, 50, 20));

				InputBlocker first = new InputBlocker(frame);
				InputBlocker second = new InputBlocker(frame);
				first.block();
				second.block();
				// moved behind the blockers, the dispatchers called in the order added
				focusManager.removeKeyEventDispatcher(last);
				focusManager.addKeyEventDispatcher(last);
				type(field, 'b');
				assertEquals(2, layeredPane.getComponentsInLayer(BLOCKING_LAYER).length);
				assertNotSame(field, SwingUtilities.getDeepestComponentAt(rootPane, 50, 20));

				// unblocked out of order, each removing only its own
				first.unblock();
				assertFalse(first.blocking());
				assertEquals(1, layeredPane.getComponentsInLayer(BLOCKING_LAYER).length);
				type(field, 'c');
				assertNotSame(field, SwingUtilities.getDeepestComponentAt(rootPane, 50, 20));

				second.unblock();
				assertEquals(0, layeredPane.getComponentsInLayer(BLOCKING_LAYER).length);
				type(field, 'd');
				assertSame(field, SwingUtilities.getDeepestComponentAt(rootPane, 50, 20));
				// a second unblock does nothing
				second.unblock();
				assertEquals(asList('a', 'd'), reached);
			}
			finally {
				focusManager.removeKeyEventDispatcher(last);
				frame.dispose();
			}
		});
	}

	private static void type(JTextField field, char character) {
		field.dispatchEvent(new KeyEvent(field, KeyEvent.KEY_TYPED, System.currentTimeMillis(), 0, KeyEvent.VK_UNDEFINED, character));
	}
}
