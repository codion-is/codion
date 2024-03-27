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
 * Copyright (c) 2023 - 2024, Björn Darri Sigurðsson.
 */
/**
 * Common Swing UI classes, such as:<br>
 * <br>
 * {@link is.codion.swing.common.ui.component.Components}<br>
 * {@link is.codion.swing.common.ui.component.builder.ComponentBuilder}<br>
 * {@link is.codion.swing.common.ui.component.builder.AbstractComponentBuilder}<br>
 * {@link is.codion.swing.common.ui.component.value.ComponentValue}<br>
 * {@link is.codion.swing.common.ui.component.value.AbstractComponentValue}<br>
 * {@link is.codion.swing.common.ui.dialog.Dialogs}<br>
 * {@link is.codion.swing.common.ui.key.KeyEvents}<br>
 * {@link is.codion.swing.common.ui.laf.LookAndFeelProvider}<br>
 * {@link is.codion.swing.common.ui.layout.Layouts}<br>
 */
module is.codion.swing.common.ui {
	requires java.rmi;
	requires transitive is.codion.swing.common.model;
	requires transitive org.kordamp.ikonli.core;
	requires org.kordamp.ikonli.swing;

	exports is.codion.swing.common.ui;
	exports is.codion.swing.common.ui.component;
	exports is.codion.swing.common.ui.component.builder;
	exports is.codion.swing.common.ui.component.button;
	exports is.codion.swing.common.ui.component.calendar;
	exports is.codion.swing.common.ui.component.combobox;
	exports is.codion.swing.common.ui.component.label;
	exports is.codion.swing.common.ui.component.list;
	exports is.codion.swing.common.ui.component.panel;
	exports is.codion.swing.common.ui.component.progressbar;
	exports is.codion.swing.common.ui.component.scrollpane;
	exports is.codion.swing.common.ui.component.slider;
	exports is.codion.swing.common.ui.component.spinner;
	exports is.codion.swing.common.ui.component.splitpane;
	exports is.codion.swing.common.ui.component.tabbedpane;
	exports is.codion.swing.common.ui.component.table;
	exports is.codion.swing.common.ui.component.text;
	exports is.codion.swing.common.ui.component.value;
	exports is.codion.swing.common.ui.border;
	exports is.codion.swing.common.ui.control;
	exports is.codion.swing.common.ui.dialog;
	exports is.codion.swing.common.ui.icon;
	exports is.codion.swing.common.ui.key;
	exports is.codion.swing.common.ui.laf;
	exports is.codion.swing.common.ui.layout;
}