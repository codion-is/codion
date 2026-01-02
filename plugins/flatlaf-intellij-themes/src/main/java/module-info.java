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
 * Copyright (c) 2023 - 2026, Björn Darri Sigurðsson.
 */
/**
 * IntelliJ themes
 */
module is.codion.plugin.flatlaf.intellij.themes {
	requires java.logging;
	requires java.desktop;
	requires is.codion.swing.common.ui;
	requires transitive com.formdev.flatlaf;

	exports is.codion.plugin.flatlaf.intellij;
	exports is.codion.plugin.flatlaf.intellij.themes.akusan;
	exports is.codion.plugin.flatlaf.intellij.themes.arc;
	exports is.codion.plugin.flatlaf.intellij.themes.autumn;
	exports is.codion.plugin.flatlaf.intellij.themes.carbon;
	exports is.codion.plugin.flatlaf.intellij.themes.catppuccin;
	exports is.codion.plugin.flatlaf.intellij.themes.cobalt2;
	exports is.codion.plugin.flatlaf.intellij.themes.codely;
	exports is.codion.plugin.flatlaf.intellij.themes.copilot;
	exports is.codion.plugin.flatlaf.intellij.themes.cutepink;
	exports is.codion.plugin.flatlaf.intellij.themes.cyan;
	exports is.codion.plugin.flatlaf.intellij.themes.cyberpunk;
	exports is.codion.plugin.flatlaf.intellij.themes.darculasolid;
	exports is.codion.plugin.flatlaf.intellij.themes.darkflat;
	exports is.codion.plugin.flatlaf.intellij.themes.darkhorizon;
	exports is.codion.plugin.flatlaf.intellij.themes.darkpurple;
	exports is.codion.plugin.flatlaf.intellij.themes.darkubuntu;
	exports is.codion.plugin.flatlaf.intellij.themes.dracula;
	exports is.codion.plugin.flatlaf.intellij.themes.elegant;
	exports is.codion.plugin.flatlaf.intellij.themes.espresso;
	exports is.codion.plugin.flatlaf.intellij.themes.everforest;
	exports is.codion.plugin.flatlaf.intellij.themes.gerry;
	exports is.codion.plugin.flatlaf.intellij.themes.github;
	exports is.codion.plugin.flatlaf.intellij.themes.gradianto;
	exports is.codion.plugin.flatlaf.intellij.themes.gray;
	exports is.codion.plugin.flatlaf.intellij.themes.gruvbox;
	exports is.codion.plugin.flatlaf.intellij.themes.halcyon;
	exports is.codion.plugin.flatlaf.intellij.themes.hardhacker;
	exports is.codion.plugin.flatlaf.intellij.themes.hiberbee;
	exports is.codion.plugin.flatlaf.intellij.themes.highcontrast;
	exports is.codion.plugin.flatlaf.intellij.themes.iceberg;
	exports is.codion.plugin.flatlaf.intellij.themes.lightflat;
	exports is.codion.plugin.flatlaf.intellij.themes.lotus;
	exports is.codion.plugin.flatlaf.intellij.themes.material;
	exports is.codion.plugin.flatlaf.intellij.themes.materialtheme;
	exports is.codion.plugin.flatlaf.intellij.themes.monocai;
	exports is.codion.plugin.flatlaf.intellij.themes.monokaipro;
	exports is.codion.plugin.flatlaf.intellij.themes.naturethemes;
	exports is.codion.plugin.flatlaf.intellij.themes.nightfall;
	exports is.codion.plugin.flatlaf.intellij.themes.nord;
	exports is.codion.plugin.flatlaf.intellij.themes.obsidiansunset;
	exports is.codion.plugin.flatlaf.intellij.themes.onedark;
	exports is.codion.plugin.flatlaf.intellij.themes.onepauintxi;
	exports is.codion.plugin.flatlaf.intellij.themes.pinkasheck;
	exports is.codion.plugin.flatlaf.intellij.themes.rider;
	exports is.codion.plugin.flatlaf.intellij.themes.solarized;
	exports is.codion.plugin.flatlaf.intellij.themes.solidcherry;
	exports is.codion.plugin.flatlaf.intellij.themes.spacegray;
	exports is.codion.plugin.flatlaf.intellij.themes.srcery;
	exports is.codion.plugin.flatlaf.intellij.themes.tayviscon;
	exports is.codion.plugin.flatlaf.intellij.themes.trashpanda;
	exports is.codion.plugin.flatlaf.intellij.themes.vscode;
	exports is.codion.plugin.flatlaf.intellij.themes.vuesion;
	exports is.codion.plugin.flatlaf.intellij.themes.xcodedark;

	provides is.codion.swing.common.ui.laf.LookAndFeelProvider
					with is.codion.plugin.flatlaf.intellij.IntelliJThemeProvider;
	provides is.codion.swing.common.ui.scaler.Scaler
					with is.codion.plugin.flatlaf.intellij.scaler.UIScaler;
}