package xyz.exelixi.frontend;

import se.lth.cs.tycho.comp.*;
import se.lth.cs.tycho.reporting.Reporter;
import se.lth.cs.tycho.settings.Configuration;
import static xyz.exelixi.Settings.*;

import java.util.Arrays;

/**
 * Created by scb on 1/26/17.
 */
public interface FrontendLoader extends Loader {

    static Loader instance(Configuration configuration, Reporter reporter) {
        return new CombinedLoader(Arrays.asList(
                new CalLoader(reporter, configuration.get(sourcePaths), configuration.get(followLinks)),
                new OrccLoader(reporter, configuration.get(orccSourcePaths), configuration.get(followLinks)),
                new XdfLoader(reporter, configuration.get(xdfSourcePaths)),
                new PreludeLoader(reporter)));
    }

}