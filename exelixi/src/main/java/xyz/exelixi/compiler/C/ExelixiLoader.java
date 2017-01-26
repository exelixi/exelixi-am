package xyz.exelixi.compiler.C;

import se.lth.cs.tycho.comp.*;
import se.lth.cs.tycho.reporting.Reporter;
import se.lth.cs.tycho.settings.Configuration;

import java.util.Arrays;

/**
 * Created by endrix on 1/9/17.
 */
public interface ExelixiLoader extends Loader {

    static Loader instance(Configuration configuration, Reporter reporter) {
        return new CombinedLoader(Arrays.asList(
                new CalLoader(reporter, configuration.get(ExelixiCompiler.sourcePaths), configuration.get(followLinks)),
                new OrccLoader(reporter, configuration.get(ExelixiCompiler.orccSourcePaths), configuration.get(followLinks)),
                new XdfLoader(reporter, configuration.get(ExelixiCompiler.xdfSourcePaths)),
                new PreludeLoader(reporter)));
    }

}
