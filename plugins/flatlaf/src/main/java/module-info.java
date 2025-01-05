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
 * Flat Look and Feel
 */
module is.codion.plugin.flatlaf {
	requires java.desktop;
	requires is.codion.swing.common.ui;
	requires transitive com.formdev.flatlaf;

	provides is.codion.swing.common.ui.laf.LookAndFeelProvider
					with is.codion.plugin.flatlaf.FlatLookAndFeelProvider;
}