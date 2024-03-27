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
 * Copyright (c) 2022 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.common.ui.icon;

import javax.swing.ImageIcon;
import java.awt.Toolkit;

/**
 * Provides logos.
 */
public interface Logos {

	/**
	 * @return icon for the codion logo
	 */
	static ImageIcon logoBlack() {
		return imageIcon("codion-logo-rounded-black-48x48.png");
	}

	/**
	 * @return icon for the codion logo
	 */
	static ImageIcon logoTransparent() {
		return imageIcon("codion-logo-transparent-48x48.png");
	}

	/**
	 * @return icon for the codion logo in red
	 */
	static ImageIcon logoRed() {
		return imageIcon("codion-logo-rounded-red-48x48.png");
	}

	static ImageIcon imageIcon(String resourceName) {
		return new ImageIcon(Toolkit.getDefaultToolkit().getImage(Logos.class.getResource(resourceName)));
	}
}
