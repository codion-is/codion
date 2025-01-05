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
 * Copyright (c) 2023 - 2025, Björn Darri Sigurðsson.
 */
/**
 * Petclinic demo.
 */
module is.codion.demos.petclinic {
	requires is.codion.swing.framework.ui;
	requires is.codion.plugin.flatlaf.intellij.themes;

	exports is.codion.demos.petclinic.model
					to is.codion.swing.framework.model, is.codion.swing.framework.ui;
	exports is.codion.demos.petclinic.ui
					to is.codion.swing.framework.ui;

	provides is.codion.framework.domain.Domain
					with is.codion.demos.petclinic.domain.PetclinicImpl;
}