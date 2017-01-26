package xyz.exelixi.backend.c.codegen;

import org.multij.Binding;
import org.multij.Module;
import xyz.exelixi.backend.c.CBackendCore;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * It prints the main.c source file
 * @author Endri Bezati
 */
@Module
public interface CMainFunction {
    @Binding
    CBackendCore backend();

    default Emitter emitter() {
        return backend().emitter();
    }

    default void generateCode() {
        //CompilationTask task = backend().task();

        //Network network = backend().task().getNetwork();
        //channels().inputActorCode();
        //channels().outputActorCode();

        try (InputStream in = ClassLoader.getSystemResourceAsStream("c_backend_code/main.c")) {
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            reader.lines().forEach(emitter()::emitRawLine);
        } catch (IOException e) {
            throw new Error(e);
        }

    }


}
