package io.hyperfoil.tools.qdup.quarkus;

import io.hyperfoil.tools.qdup.Run;
import io.hyperfoil.tools.qdup.SshTestBase;
import io.hyperfoil.tools.qdup.Stage;
import io.hyperfoil.tools.qdup.cmd.Dispatcher;
import io.hyperfoil.tools.qdup.config.RunConfig;
import io.hyperfoil.tools.qdup.config.RunConfigBuilder;
import io.hyperfoil.tools.qdup.config.yaml.Parser;
import org.junit.Test;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.lang.invoke.MethodHandles;
import java.util.stream.Collectors;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class QuarkusGettingStartedTest extends SshTestBase {

    final static XLogger logger = XLoggerFactory.getXLogger(MethodHandles.lookup().lookupClass());

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
                      - sh: "source ~/.bashrc" # jbang change your environment if you don't have jbang in the PATH
                  getting-started:
                  - sh: cd /tmp/
                  - sh: "[[ ! -d quarkus-quickstarts ]] && git clone https://github.com/quarkusio/quarkus-quickstarts.git" # prevent cloning again
                  - sh: cd quarkus-quickstarts
                  - sh: cd getting-started
                  - script: ensure-quarkus-cli
                  - sh:
                      command: quarkus dev
                      prompt:
                        "Press [space] to restart, [e] to edit command line args (currently ''), [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>": "r"
                    watch:
                      - regex: "Tests paused"
                        then:
                        - signal: ready
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
        logger.info("storing artifacts at: {}", tmpDir.toString());
        Run doit = new Run(tmpDir.toString(), config, dispatcher);
        doit.run();
        assertTrue(doit.getStage().equals(Stage.Done));
    }

    @Test
    public void testStep3() {
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = getBuilder();
        builder.loadYaml(parser.loadFile("signal",stream(
                """
                    scripts:
                      test-source:
                      - sh: source ~/.bashrc
                    hosts:
                      test: ${{HOST}}
                    roles:
                      run-example:
                        hosts:
                        - test
                        run-scripts:
                        - test-source
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
    public void testStep4() {
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = getBuilder();
        builder.loadYaml(parser.loadFile("signal",stream(
                """
                    scripts:
                      test-endpoint:
                      - wait-for: ready
                        then:
                        - sh: curl localhost:8080/hello
                        - signal: done #tells qDup that the "testing" is done
                      ensure-quarkus-cli:
                      - sh: quarkus version #to see if the command exists
                        then:
                        - regex: "command not found"
                          then: #install quarkus by following: https://quarkus.io/get-started/
                          - sh: "curl -Ls https://sh.jbang.dev | bash -s - trust add https://repo1.maven.org/maven2/io/quarkus/quarkus-cli/"
                          - sh: "curl -Ls https://sh.jbang.dev | bash -s - app install --fresh --force quarkus@quarkusio"
                          - sh: "source ~/.bashrc" # jbang change your environment if you don't have jbang in the PATH
                      getting-started:
                      - sh: cd /tmp/
                      - sh: "[[ ! -d quarkus-quickstarts ]] && git clone https://github.com/quarkusio/quarkus-quickstarts.git" # prevent cloning again
                      - sh: cd quarkus-quickstarts
                      - sh: cd getting-started
                      - script: ensure-quarkus-cli
                      - sh:
                          command: quarkus dev
                          prompt:
                            "Press [e] to edit command line args (currently ''), [r] to resume testing, [o] Toggle test output, [:] for the terminal, [h] for more options>": "r"
                        watch:
                        - regex: "Tests completed"
                          then:
                          - signal: ready
                        on-signal:
                          done:
                          - ctrlC #exits the process
                    hosts:
                      test: ${{HOST}}
                    roles:
                      setup-env:
                        hosts:
                        - test
                        run-scripts:
                        - getting-started
                        - test-endpoint
                    states:
                      HOST: LOCAL
                    """
        )));
        RunConfig config = builder.buildConfig(parser);
        String message = "runConfig errors:\n" + config.getErrorStrings().stream().collect(Collectors.joining("\n"));
        assertFalse(message, config.hasErrors());

        Dispatcher dispatcher = new Dispatcher();
        logger.info("storing artifacts at: {}", tmpDir.toString());
        Run doit = new Run(tmpDir.toString(), config, dispatcher);
        doit.run();
        assertTrue(doit.getStage().equals(Stage.Done));
    }

    @Test
    public void testStep5() {
        Parser parser = Parser.getInstance();
        RunConfigBuilder builder = getBuilder();
        builder.loadYaml(parser.loadFile("signal",stream(
                """
                    scripts:
                      getting-started:
                      - sh:
                          command: sh /home/dlovison/diego-kitchen/qDup-examples/read_input.sh
                          prompt:
                            "Enter fullname: ": "diego lovison"
                            "Enter user: ": "dlovison"
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
        logger.info("storing artifacts at: {}", tmpDir.toString());
        Run doit = new Run(tmpDir.toString(), config, dispatcher);
        doit.run();
        assertTrue(doit.getStage().equals(Stage.Done));
    }
}
