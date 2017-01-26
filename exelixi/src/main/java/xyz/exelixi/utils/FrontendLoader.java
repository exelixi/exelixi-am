/*
 * EXELIXI
 *
 * Copyright (C) 2017 Endri Bezati, EPFL SCI-STI-MM
 *
 * This file is part of EXELIXI.
 *
 * EXELIXI is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * EXELIXI is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with EXELIXI. If not, see <http://www.gnu.org/licenses/>.
 *
 * Additional permission under GNU GPL version 3 section 7
 *
 * If you modify this Program, or any covered work, by linking or combining it
 * with Eclipse (or a modified version of Eclipse or an Eclipse plugin or
 * an Eclipse library), containing parts covered by the terms of the
 * Eclipse Public License (EPL), the licensors of this Program grant you
 * additional permission to convey the resulting work.  Corresponding Source
 * for a non-source form of such a combination shall include the source code
 * for the parts of Eclipse libraries used as well as that of the covered work.
 *
 */

package xyz.exelixi.utils;

import se.lth.cs.tycho.comp.*;
import se.lth.cs.tycho.reporting.Reporter;
import se.lth.cs.tycho.settings.Configuration;
import xyz.exelixi.Settings;

import java.util.Arrays;

/**
 * Exelixi HLS Loader
 *
 * @author Endri Bezati
 * @author Gustav Cedersjo
 */
public interface FrontendLoader extends Loader {

    static Loader instance(Configuration configuration, Reporter reporter) {
        return new CombinedLoader(Arrays.asList(
                new CalLoader(reporter, configuration.get(Settings.sourcePaths), configuration.get(followLinks)),
                new OrccLoader(reporter, configuration.get(Settings.orccSourcePaths), configuration.get(followLinks)),
                new XdfLoader(reporter, configuration.get(Settings.xdfSourcePaths)),
                new PreludeLoader(reporter)));
    }

}
