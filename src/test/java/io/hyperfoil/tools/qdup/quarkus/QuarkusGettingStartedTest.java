package io.hyperfoil.tools.qdup.quarkus;

import io.hyperfoil.tools.qdup.Run;
import io.hyperfoil.tools.qdup.SshTestBase;
import io.hyperfoil.tools.qdup.Stage;
import io.hyperfoil.tools.qdup.cmd.Dispatcher;
import io.hyperfoil.tools.qdup.config.RunConfig;
import io.hyperfoil.tools.qdup.config.RunConfigBuilder;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import org.junit.Test;
import java.util.stream.Collectors;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuarkusGettingStartedTest extends SshTestBase {

    @Test
    public void testStep1() {
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = getBuilder();
        builder.loadYaml(parser.loadFile("signal",stream(
        """
         scripts:
           start-script:
             - log: "Running script"
             - sh: echo "Hello World!"
             - log: "Finished script"
         
         hosts:
           target-host: ${{HOST}}
         
         roles:
           db:
             hosts:
               - target-host
             setup-scripts:
               - start-script
         
         states:
           HOST: LOCAL
         """
        )));
        RunConfig config = builder.buildConfig(parser);
        String message = "runConfig errors:\n" + config.getErrorStrings().stream().collect(Collectors.joining("\n"));
        assertFalse(message, config.hasErrors());

        Dispatcher dispatcher = new Dispatcher();
        Run doit = new Run(tmpDir.toString(), config, dispatcher);
        doit.run();
        assertTrue(doit.getStage().equals(Stage.Done));
    }
}
