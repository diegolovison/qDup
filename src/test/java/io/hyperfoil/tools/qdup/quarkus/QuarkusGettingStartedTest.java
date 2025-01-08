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

    @Test
    public void testStep2() {
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = getBuilder();
        builder.loadYaml(parser.loadFile("signal",stream(
            """
                scripts:
                  ensure-quarkus-cli:
                  - sh: quarkus version #to see if the command exists
                    then:
                    - regex: "command not found"
                      then: #install quarkus by following: https://quarkus.io/get-started/
                      - sh: "curl -Ls https://sh.jbang.dev | bash -s - trust add https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/"
                      - sh: "curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --force quarkus@quarkusio"
                  getting-started:
                  - sh: cd /tmp/
                  - sh: "[[ ! -d quarkus-quickstarts ]] && git clone https://github.com/quarkusio/quarkus-quickstarts.git" # prevent cloning again
                  - sh: cd quarkus-quickstarts
                  - sh: cd getting-started
                  - script: ensure-quarkus-cli
                  - sh: quarkus dev
                hosts:
                  test: ${{HOST}}
                roles:
                  setup-env:
                    hosts:
                    - test
                    run-scripts:
                    - getting-started
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
