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
 * Copyright (c) 2020 - 2024, Björn Darri Sigurðsson.
 */
package is.codion.swing.framework.ui;

import is.codion.common.Configuration;
import is.codion.common.property.PropertyValue;

/**
 * The possible actions to take on a referential integrity error.
 */
public enum ReferentialIntegrityErrorHandling {
	/**
	 * Display the error.
	 */
	DISPLAY_ERROR,
	/**
	 * Display the dependencies causing the error.
	 */
	DISPLAY_DEPENDENCIES;

	/**
	 * Specifies whether to display the error message or the dependent entities in case of a referential integrity error on delete
	 * <ul>
	 * <li>Value type: {@link ReferentialIntegrityErrorHandling}
	 * <li>Default value: {@link ReferentialIntegrityErrorHandling#DISPLAY_ERROR}
	 * </ul>
	 */
	public static final PropertyValue<ReferentialIntegrityErrorHandling> REFERENTIAL_INTEGRITY_ERROR_HANDLING =
					Configuration.enumValue(ReferentialIntegrityErrorHandling.class.getName() + ".referentialIntegrityErrorHandling",
									ReferentialIntegrityErrorHandling.class, ReferentialIntegrityErrorHandling.DISPLAY_ERROR);
}
