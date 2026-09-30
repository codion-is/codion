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
package is.codion.swing.common.ui.component.splitpane;

import org.junit.jupiter.api.Test;

import javax.swing.JLabel;
import javax.swing.JSplitPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;

import static org.junit.jupiter.api.Assertions.*;

public final class SplitPaneBuilderTest {

	@Test
	void dividerLocation() {
		JSplitPane splitPane = SplitPaneBuilder.builder()
						.leftComponent(new JLabel("left"))
						.rightComponent(new JLabel("right"))
						.dividerLocation(120)
						.build();
		assertEquals(120, splitPane.getDividerLocation());
	}

	@Test
	void continuousLayout() {
		Object continuousLayout = UIManager.get("SplitPane.continuousLayout");
		try {
			// the look and feel default, as FlatLaf's
			UIManager.put("SplitPane.continuousLayout", true);
			assertTrue(SplitPaneBuilder.builder().build().isContinuousLayout());
			assertFalse(SplitPaneBuilder.builder().continuousLayout(false).build().isContinuousLayout());
		}
		finally {
			UIManager.put("SplitPane.continuousLayout", continuousLayout);
		}
	}

	@Test
	void proportionalDividerLocation() throws Exception {
		assertProportional(JSplitPane.HORIZONTAL_SPLIT);
		assertProportional(JSplitPane.VERTICAL_SPLIT);
		assertThrows(IllegalArgumentException.class, () -> SplitPaneBuilder.builder().dividerLocation(-0.1));
		assertThrows(IllegalArgumentException.class, () -> SplitPaneBuilder.builder().dividerLocation(1.1));
	}

	private static void assertProportional(int orientation) throws Exception {
		JSplitPane splitPane = SplitPaneBuilder.builder()
						.orientation(orientation)
						.leftComponent(new JLabel("left"))
						.rightComponent(new JLabel("right"))
						.dividerLocation(0.25)
						.build();
		int listeners = splitPane.getComponentListeners().length;
		int location = splitPane.getDividerLocation();
		// no effect before the split pane has a size
		SwingUtilities.invokeAndWait(() -> {});
		assertEquals(location, splitPane.getDividerLocation());
		// set once it has one, the listener removed
		SwingUtilities.invokeAndWait(() -> splitPane.setSize(420, 220));
		SwingUtilities.invokeAndWait(() -> {});
		int size = orientation == JSplitPane.HORIZONTAL_SPLIT ? 420 : 220;
		assertEquals((int) ((size - splitPane.getDividerSize()) * 0.25), splitPane.getDividerLocation());
		assertEquals(listeners - 1, splitPane.getComponentListeners().length);
	}
}
