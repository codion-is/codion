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
 * Copyright (c) 2025, Björn Darri Sigurðsson.
 */
package is.codion.plugin.flatlaf.intellij;

import is.codion.common.utilities.exceptions.Exceptions;
import is.codion.plugin.flatlaf.intellij.themes.akusan.AkusanDark;
import is.codion.plugin.flatlaf.intellij.themes.akusan.AkusanGray;
import is.codion.plugin.flatlaf.intellij.themes.akusan.AkusanLight;
import is.codion.plugin.flatlaf.intellij.themes.arc.Arc;
import is.codion.plugin.flatlaf.intellij.themes.arc.ArcDark;
import is.codion.plugin.flatlaf.intellij.themes.arc.ArcDarkOrange;
import is.codion.plugin.flatlaf.intellij.themes.arc.ArcOrange;
import is.codion.plugin.flatlaf.intellij.themes.carbon.Carbon;
import is.codion.plugin.flatlaf.intellij.themes.catppuccin.CatppuccinFrappe;
import is.codion.plugin.flatlaf.intellij.themes.catppuccin.CatppuccinLatte;
import is.codion.plugin.flatlaf.intellij.themes.catppuccin.CatppuccinMacchiato;
import is.codion.plugin.flatlaf.intellij.themes.catppuccin.CatppuccinMocha;
import is.codion.plugin.flatlaf.intellij.themes.cobalt2.Cobalt2;
import is.codion.plugin.flatlaf.intellij.themes.codely.Codely;
import is.codion.plugin.flatlaf.intellij.themes.codely.CodelyBlue;
import is.codion.plugin.flatlaf.intellij.themes.codely.CodelyDark;
import is.codion.plugin.flatlaf.intellij.themes.codely.CodelyLight;
import is.codion.plugin.flatlaf.intellij.themes.cutepink.CutePinkDark;
import is.codion.plugin.flatlaf.intellij.themes.cutepink.CutePinkLight;
import is.codion.plugin.flatlaf.intellij.themes.cyberpunk.Cyberpunk;
import is.codion.plugin.flatlaf.intellij.themes.darculasolid.DarculaSolid;
import is.codion.plugin.flatlaf.intellij.themes.darkflat.DarkFlat;
import is.codion.plugin.flatlaf.intellij.themes.darkhorizon.DarkHorizon;
import is.codion.plugin.flatlaf.intellij.themes.darkpurple.DarkPurple;
import is.codion.plugin.flatlaf.intellij.themes.dracula.Dracula;
import is.codion.plugin.flatlaf.intellij.themes.elegant.ElegantDark;
import is.codion.plugin.flatlaf.intellij.themes.elegant.ElegantLight;
import is.codion.plugin.flatlaf.intellij.themes.espresso.EspressoLight;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestDarkHard;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestDarkMedium;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestDarkSoft;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestLightHard;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestLightMedium;
import is.codion.plugin.flatlaf.intellij.themes.everforest.EverforestLightSoft;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryDarcula;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryDark;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryLight;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryOceanic;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryStorm;
import is.codion.plugin.flatlaf.intellij.themes.gerry.GerryViolet;
import is.codion.plugin.flatlaf.intellij.themes.github.GithubDark;
import is.codion.plugin.flatlaf.intellij.themes.github.GithubDarkDefault;
import is.codion.plugin.flatlaf.intellij.themes.github.GithubLight;
import is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoDarkFuchsia;
import is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoDeepOcean;
import is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoMidnightBlue;
import is.codion.plugin.flatlaf.intellij.themes.gradianto.GradiantoNatureGreen;
import is.codion.plugin.flatlaf.intellij.themes.gray.Gray;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxDarkHard;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxDarkMedium;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxDarkSoft;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxMaterialLightHard;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxMaterialLightMedium;
import is.codion.plugin.flatlaf.intellij.themes.gruvbox.GruvboxMaterialLightSoft;
import is.codion.plugin.flatlaf.intellij.themes.halcyon.Halcyon;
import is.codion.plugin.flatlaf.intellij.themes.hiberbee.HiberbeeDark;
import is.codion.plugin.flatlaf.intellij.themes.highcontrast.HighContrast;
import is.codion.plugin.flatlaf.intellij.themes.lightflat.LightFlat;
import is.codion.plugin.flatlaf.intellij.themes.lotus.LotusDark;
import is.codion.plugin.flatlaf.intellij.themes.lotus.LotusLight;
import is.codion.plugin.flatlaf.intellij.themes.material.ArcDarkMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.AtomOneDark;
import is.codion.plugin.flatlaf.intellij.themes.material.AtomOneLight;
import is.codion.plugin.flatlaf.intellij.themes.material.DraculaMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.GitHubDarkMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.GitHubMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.LightOwl;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialDarker;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialDeepOcean;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialLighter;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialOceanic;
import is.codion.plugin.flatlaf.intellij.themes.material.MaterialPalenight;
import is.codion.plugin.flatlaf.intellij.themes.material.MonokaiProMaterial;
import is.codion.plugin.flatlaf.intellij.themes.material.Moonlight;
import is.codion.plugin.flatlaf.intellij.themes.material.NightOwl;
import is.codion.plugin.flatlaf.intellij.themes.material.SolarizedDark;
import is.codion.plugin.flatlaf.intellij.themes.material.SolarizedLight;
import is.codion.plugin.flatlaf.intellij.themes.materialtheme.MaterialTheme;
import is.codion.plugin.flatlaf.intellij.themes.monocai.Monocai;
import is.codion.plugin.flatlaf.intellij.themes.monokaipro.MonokaiPro;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.AuroraBorealis;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Autumn;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Everest;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Roraima;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Sakura;
import is.codion.plugin.flatlaf.intellij.themes.naturethemes.Sequoia;
import is.codion.plugin.flatlaf.intellij.themes.nightfall.Nightfall;
import is.codion.plugin.flatlaf.intellij.themes.obsidiansunset.ObsidianSunsetBlue;
import is.codion.plugin.flatlaf.intellij.themes.obsidiansunset.ObsidianSunsetOrange;
import is.codion.plugin.flatlaf.intellij.themes.onedark.OneDark;
import is.codion.plugin.flatlaf.intellij.themes.onepauintxi.OnePauintxiAllBlue;
import is.codion.plugin.flatlaf.intellij.themes.onepauintxi.OnePauintxiAllOrange;
import is.codion.plugin.flatlaf.intellij.themes.onepauintxi.OnePauintxiBlue;
import is.codion.plugin.flatlaf.intellij.themes.onepauintxi.OnePauintxiOrange;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderDark;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderDay;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderLight;
import is.codion.plugin.flatlaf.intellij.themes.rider.RiderNight;
import is.codion.plugin.flatlaf.intellij.themes.srcery.Srcery;
import is.codion.plugin.flatlaf.intellij.themes.tayviscon.TeyvisconDark;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPanda;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaBlacklight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaDawnlight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaDaylight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaMoonlight;
import is.codion.plugin.flatlaf.intellij.themes.trashpanda.TrashPandaStarlight;
import is.codion.plugin.flatlaf.intellij.themes.vscode.VSCodeDark;
import is.codion.plugin.flatlaf.intellij.themes.vscode.VSCodeDarkModern;
import is.codion.plugin.flatlaf.intellij.themes.vscode.VSCodeLight;
import is.codion.plugin.flatlaf.intellij.themes.vuesion.Vuesion;
import is.codion.plugin.flatlaf.intellij.themes.xcodedark.XcodeDark;
import is.codion.swing.common.ui.Utilities;
import is.codion.swing.common.ui.laf.LookAndFeelEnabler;
import is.codion.swing.common.ui.laf.LookAndFeelProvider;
import is.codion.swing.common.ui.scaler.Scaler;

import com.formdev.flatlaf.FlatLaf;
import com.formdev.flatlaf.util.LoggingFacade;

import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import java.util.Collection;
import java.util.function.Consumer;
import java.util.logging.Level;
import java.util.logging.Logger;

import static is.codion.swing.common.ui.laf.LookAndFeelEnabler.lookAndFeelEnabler;
import static java.util.Arrays.asList;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.requireNonNull;

/**
 * <p>Provides Look And Feels based on IntelliJ themes.
 * <p>Uses Flat Look And Feel under the hood.
 */
public final class IntelliJThemeProvider implements LookAndFeelProvider {

	private static final Consumer<LookAndFeelInfo> ENABLER = new DefaultEnabler();

	private final Collection<LookAndFeelEnabler> enablers;

	static {
		// Turn off FlatLaf logging to get around
		// https://github.com/JFormDesigner/FlatLaf/issues/990
		LoggingFacade facade = LoggingFacade.INSTANCE;
		Logger.getLogger(FlatLaf.class.getName()).setLevel(Level.OFF);
	}

	public IntelliJThemeProvider() {
		enablers = unmodifiableList(asList(
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Aurora Borealis", AuroraBorealis.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Autumn", Autumn.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Everest", Everest.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Roraima", Roraima.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Sakura", Sakura.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Nature Sequoia", Sequoia.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Srcery", Srcery.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Darcula", GerryDarcula.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Dark", GerryDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Oceanic", GerryOceanic.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Storm", GerryStorm.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Violet", GerryViolet.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gerry Light", GerryLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Lotus Dark", LotusDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Lotus Light", LotusLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Teyviscon Dark", TeyvisconDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Dark Hard", EverforestDarkHard.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Dark Medium", EverforestDarkMedium.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Dark Soft", EverforestDarkSoft.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Light Hard", EverforestLightHard.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Light Medium", EverforestLightMedium.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Everforest Light Soft", EverforestLightSoft.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Dark Horizon", DarkHorizon.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Akusan Dark", AkusanDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Akusan Gray", AkusanGray.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Akusan Light", AkusanLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("One Pauintxi All Blue", OnePauintxiAllBlue.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("One Pauintxi Blue", OnePauintxiBlue.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("One Pauintxi All Orange", OnePauintxiAllOrange.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("One Pauintxi Orange", OnePauintxiOrange.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Light Hard", GruvboxMaterialLightHard.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Light Medium", GruvboxMaterialLightMedium.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Light Soft", GruvboxMaterialLightSoft.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Obsidian Sunset Blue", ObsidianSunsetBlue.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Obsidian Sunset Orange", ObsidianSunsetOrange.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Github Dark", GithubDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Github Dark Default", GithubDarkDefault.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Github Light", GithubLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Dark", RiderDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Day", RiderDay.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Light", RiderLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Night", RiderNight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Rider Dark", RiderDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Darcula Solid", DarculaSolid.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Nightfall", Nightfall.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda", TrashPanda.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Blacklight", TrashPandaBlacklight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Dawnlight", TrashPandaDawnlight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Daylight", TrashPandaDaylight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Moonlight", TrashPandaMoonlight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Trash Panda Starlight", TrashPandaStarlight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Cute Pink Light", CutePinkLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Cute Pink Dark", CutePinkDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Codely", Codely.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Codely Blue", CodelyBlue.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Codely Dark", CodelyDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Codely Light", CodelyLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Cyberpunk", Cyberpunk.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Elegant Dark", ElegantDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Elegant Light", ElegantLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Espresso Light", EspressoLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Dracula", Dracula.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Catppuccin Frappe", CatppuccinFrappe.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Catppuccin Latte", CatppuccinLatte.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Catppuccin Macchiato", CatppuccinMacchiato.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Catppuccin Mocha", CatppuccinMocha.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("VSCode Dark", VSCodeDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("VSCode Dark Modern", VSCodeDarkModern.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("VSCode Light", VSCodeLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Halcyon", Halcyon.class.getName()), ENABLER),

						// From FlatLaf
						lookAndFeelEnabler(new LookAndFeelInfo("Arc Dark (Material)", ArcDarkMaterial.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Atom One Dark (Material)", AtomOneDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Atom One Light (Material)", AtomOneLight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Dracula (Material)", DraculaMaterial.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Github Dark (Material)", GitHubDarkMaterial.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Github (Material)", GitHubMaterial.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Light Owl (Material)", LightOwl.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Darker (Material)", MaterialDarker.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Deep Ocean (Material)", MaterialDeepOcean.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Lighter (Material)", MaterialLighter.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Oceanic (Material)", MaterialOceanic.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Palenight (Material)", MaterialPalenight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Monokai Pro (Material)", MonokaiProMaterial.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Moonlight (Material)", Moonlight.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Night Owl (Material)", NightOwl.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Solarized Dark (Material)", SolarizedDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Solarized Light (Material)", SolarizedLight.class.getName()), ENABLER),
						// End material

						lookAndFeelEnabler(new LookAndFeelInfo("Arc", Arc.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Arc Dark", ArcDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Arc Orance", ArcOrange.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Arc Dark Orange", ArcDarkOrange.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Carbon", Carbon.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Cobalt 2", Cobalt2.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Dark Flat", DarkFlat.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Dark Purple", DarkPurple.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gradianto Dark Fuchsia", GradiantoDarkFuchsia.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gradianto Deep Ocean", GradiantoDeepOcean.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gradianto Midnight Blue", GradiantoMidnightBlue.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gradianto Nature Green", GradiantoNatureGreen.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gray", Gray.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Dark Hard", GruvboxDarkHard.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Dark Medium", GruvboxDarkMedium.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Gruvbox Dark Soft", GruvboxDarkSoft.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Hiberbee Dark", HiberbeeDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("High Contrast", HighContrast.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Light Flat", LightFlat.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Material Theme", MaterialTheme.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Monocai", Monocai.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Monokai Pro", MonokaiPro.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("One Dark", OneDark.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("Vuesion", Vuesion.class.getName()), ENABLER),
						lookAndFeelEnabler(new LookAndFeelInfo("XCode Dark", XcodeDark.class.getName()), ENABLER)
		));
	}

	/**
	 * @return all available IntelliJ Theme Look and Feels
	 */
	public Collection<LookAndFeelEnabler> get() {
		return enablers;
	}

	/**
	 * Requrired since scaling must happen before the look and feel is applied.
	 */
	private static final class DefaultEnabler implements Consumer<LookAndFeelInfo> {

		@Override
		public void accept(LookAndFeelInfo lookAndFeelInfo) {
			try {
				Scaler.instance(requireNonNull(lookAndFeelInfo).getClassName()).ifPresent(Scaler::apply);
				UIManager.setLookAndFeel(lookAndFeelInfo.getClassName());
				Utilities.updateComponentTreeForAllWindows();
			}
			catch (Exception e) {
				throw Exceptions.runtime(e);
			}
		}
	}
}
