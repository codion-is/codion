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
package is.codion.plugin.imagepanel;

import org.junit.jupiter.api.Test;

import java.io.IOException;

import static is.codion.plugin.imagepanel.NavigableImagePanel.readImage;

public class NavigableImagePanelTest {

	@Test
	void test() throws IOException {
		NavigableImagePanel panel = new NavigableImagePanel(readImage("../../documentation/src/docs/asciidoc/images/chinook-client.png"));
		panel.setZoom(2.0);
	}
}
