package org.nrg.containers.model;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.hamcrest.CustomTypeSafeMatcher;
import org.hamcrest.Matcher;
import org.hamcrest.Matchers;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.nrg.containers.config.CommandTestConfig;
import org.nrg.containers.model.command.auto.Command;
import org.nrg.containers.model.command.auto.Command.CommandInput;
import org.nrg.containers.model.command.auto.Command.CommandMount;
import org.nrg.containers.model.command.auto.Command.CommandOutput;
import org.nrg.containers.model.command.auto.Command.CommandWrapper;
import org.nrg.containers.model.command.auto.Command.CommandWrapperDerivedInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperExternalInput;
import org.nrg.containers.model.command.auto.Command.CommandWrapperOutput;
import org.nrg.containers.model.command.entity.*;
import org.nrg.containers.services.CommandEntityService;
import org.nrg.containers.utils.TestingUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;

import java.io.File;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.List;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

@RunWith(SpringJUnit4ClassRunner.class)
@Transactional
@ContextConfiguration(classes = CommandTestConfig.class)
public class CommandEntityTest {

    private Command COMMAND;
    private CommandEntity COMMAND_ENTITY;

    @Autowired private ObjectMapper mapper;
    @Autowired private CommandEntityService commandEntityService;

    @Rule public ExpectedException expectedException = ExpectedException.none();

    @Before
    public void setup() throws Exception {
        final String outputMountName = "out";
        final CommandMount mountIn = CommandMount.create("in", false, "/input");
        final CommandMount mountOut = CommandMount.create(outputMountName, true, "/output");

        final String stringInputName = "foo";
        final CommandInput stringInput = CommandInput.builder()
                .name(stringInputName)
                .description("A foo that bars")
                .required(false)
                .defaultValue("bar")
                .commandLineFlag("--flag")
                .commandLineSeparator("=")
                .build();
        final CommandInput coolInput = CommandInput.builder()
                .name("my_cool_input")
                .description("A boolean value")
                .type("boolean")
                .required(true)
                .trueValue("-b")
                .falseValue("")
                .build();

        final String commandOutputName = "the_output";
        final CommandOutput commandOutput = CommandOutput.builder()
                .name(commandOutputName)
                .description("It's the output")
                .mount(outputMountName)
                .path("relative/path/to/dir")
                .build();

        final String externalInputName = "session";
        final CommandWrapperExternalInput externalInput = CommandWrapperExternalInput.builder()
                .name(externalInputName)
                .type("Session")
                .build();

        final String derivedInputName = "label";
        final String xnatObjectProperty = "label";
        final CommandWrapperDerivedInput derivedInput = CommandWrapperDerivedInput.builder()
                .name(derivedInputName)
                .type("string")
                .derivedFromWrapperInput(externalInputName)
                .derivedFromXnatObjectProperty(xnatObjectProperty)
                .providesValueForCommandInput(stringInputName)
                .build();

        final String outputHandlerName = "output-handler-name";
        final String outputHandlerLabel = "a_label";
        final CommandWrapperOutput outputHandler = CommandWrapperOutput.builder()
                .name(outputHandlerName)
                .commandOutputName(commandOutputName)
                .targetName(externalInputName)
                .type("Resource")
                .label(outputHandlerLabel)
                .build();

        final String commandWrapperName = "wrappername";
        final String commandWrapperDesc = "the wrapper description";
        final CommandWrapper commandWrapper = CommandWrapper.builder()
                .name(commandWrapperName)
                .description(commandWrapperDesc)
                .addExternalInput(externalInput)
                .addDerivedInput(derivedInput)
                .addOutputHandler(outputHandler)
                .build();

        COMMAND = Command.builder()
                .name("docker_image_command")
                .description("Docker Image command for the test")
                .image("abc123")
                .type("docker")
                .infoUrl("http://abc.xyz")
                .addEnvironmentVariable("foo", "bar")
                .commandLine("cmd #foo# #my_cool_input#")
                .reserveMemory(4000L)
                .limitMemory(8000L)
                .limitCpu(0.5D)
                .addMount(mountIn)
                .addMount(mountOut)
                .addInput(coolInput)
                .addInput(stringInput)
                .addOutput(commandOutput)
                .addPort("22", "2222")
                .addCommandWrapper(commandWrapper)
                .build();

        COMMAND_ENTITY = CommandEntity.fromPojo(COMMAND);

    }

    @Test
    public void testSpringConfiguration() {
        assertThat(commandEntityService, not(nullValue()));
    }

    @Test
    public void testSerializeDeserializeCommand() throws Exception {
        assertThat(mapper.readValue(mapper.writeValueAsString(COMMAND), Command.class), is(COMMAND));
    }

    @Test
    @DirtiesContext
    public void testPersistCommandWithWrapper() throws Exception {
        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestingUtils.commitTransaction();

        final CommandEntity retrievedCommandEntity = commandEntityService.retrieve(created.getId());

        assertThat(retrievedCommandEntity, is(created));
        assertThat(Command.create(created).validate(), is(Matchers.<String>emptyIterable()));

        final List<CommandWrapperEntity> commandWrappers = retrievedCommandEntity.getCommandWrapperEntities();
        assertThat(commandWrappers, hasSize(1));

        final CommandWrapperEntity commandWrapperEntity = commandWrappers.get(0);
        assertThat(commandWrapperEntity.getId(), not(0L));
        assertThat(commandWrapperEntity.getCommandEntity(), is(created));
    }

    @Test
    @DirtiesContext
    public void testDeleteCommandWithWrapper() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestingUtils.commitTransaction();

        commandEntityService.delete(created);

        TestingUtils.commitTransaction();

        assertThat(commandEntityService.retrieve(created.getId()), is(nullValue()));
    }

    @Test
    @DirtiesContext
    public void testRetrieveCommandWrapper() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestingUtils.commitTransaction();

        final CommandWrapperEntity createdWrapper = created.getCommandWrapperEntities().get(0);
        final long wrapperId = createdWrapper.getId();
        assertThat(commandEntityService.retrieveWrapper(wrapperId), is(createdWrapper));

        assertThat(Command.create(created).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testAddCommandWrapper() throws Exception {

        final CommandWrapperEntity toAdd = COMMAND_ENTITY.getCommandWrapperEntities().get(0);
        COMMAND_ENTITY.setCommandWrapperEntities(null);

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestingUtils.commitTransaction();

        final CommandWrapperEntity added = commandEntityService.addWrapper(created, toAdd);

        TestingUtils.commitTransaction();

        final CommandEntity retrieved = commandEntityService.get(COMMAND_ENTITY.getId());
        assertThat(retrieved.getCommandWrapperEntities().get(0), is(added));

        assertThat(Command.create(retrieved).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testUpdateCommandWrapperDescription() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestingUtils.commitTransaction();

        final CommandWrapperEntity createdWrapper = created.getCommandWrapperEntities().get(0);

        final String newDescription = "This is probably a new description, right?";
        createdWrapper.setDescription(newDescription);

        commandEntityService.update(created);
        TestingUtils.commitTransaction();

        final CommandEntity retrieved = commandEntityService.get(created.getId());
        final CommandWrapperEntity retrievedWrapper = retrieved.getCommandWrapperEntities().get(0);

        assertThat(retrievedWrapper.getDescription(), is(newDescription));
        assertThat(Command.create(retrieved).validate(), is(Matchers.<String>emptyIterable()));
    }

    @Test
    @DirtiesContext
    public void testUpdateAddInput() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestingUtils.commitTransaction();

        final CommandInput inputToAdd = CommandInput.builder()
                .name("this_is_new")
                .description("A new input that didn't exist before")
                .commandLineFlag("--flag")
                .commandLineSeparator("=")
                .defaultValue("yes")
                .build();
        created.addInput(CommandInputEntity.fromPojo(inputToAdd));

        commandEntityService.update(created);
        TestingUtils.commitTransaction();

        final CommandEntity retrieved = commandEntityService.get(created.getId());

        final Command retrievedPojo = Command.create(retrieved);
        assertThat(inputToAdd, isInIgnoreId(retrievedPojo.inputs()));
        assertThat(retrievedPojo.validate(), is(Matchers.<String>emptyIterable()));
    }

    private Matcher<CommandInput> isInIgnoreId(final List<CommandInput> expected) {
        final String description = "a CommandInput equal to (other than the ID) one of " + expected;
        return new CustomTypeSafeMatcher<CommandInput>(description) {
            @Override
            protected boolean matchesSafely(final CommandInput actual) {
                for (final CommandInput input : expected) {
                    final CommandInput actualWithSameId =
                            actual.toBuilder().id(input.id()).build();
                    if (input.equals(actualWithSameId)) {
                        return true;
                    }
                }
                return false;
            }
        };
    }

    @Test
    @DirtiesContext
    public void testDeleteCommandWrapper() throws Exception {

        final CommandEntity created = commandEntityService.create(COMMAND_ENTITY);

        TestingUtils.commitTransaction();

        final long wrapperId = created.getCommandWrapperEntities().get(0).getId();
        commandEntityService.deleteWrapper(wrapperId);

        TestingUtils.commitTransaction();

        assertThat(commandEntityService.retrieveWrapper(wrapperId), is(nullValue()));
    }

    @Test
    @DirtiesContext
    public void testRemoveEntitiesFromCommand() throws Exception {
        final String outputMountName = "out";
        final CommandMount mountIn = CommandMount.create("in2", false, "/input2");

        final String stringInputName = "foo2";
        final CommandInput stringInput = CommandInput.builder()
                .name(stringInputName)
                .description("A foo that bars")
                .required(false)
                .defaultValue("bar")
                .commandLineFlag("--flag")
                .commandLineSeparator("=")
                .build();

        final String commandOutputName = "the_output2";
        final CommandOutput commandOutput = CommandOutput.builder()
                .name(commandOutputName)
                .description("It's the output")
                .mount(outputMountName)
                .path("relative/path/to/dir")
                .build();

        final String externalInputName = "session";
        final CommandWrapperExternalInput externalInput = CommandWrapperExternalInput.builder()
                .name(externalInputName)
                .type("Session")
                .build();

        final String derivedInputName = "label2";
        final String xnatObjectProperty = "label";
        final CommandWrapperDerivedInput derivedInput = CommandWrapperDerivedInput.builder()
                .name(derivedInputName)
                .type("string")
                .derivedFromWrapperInput(externalInputName)
                .derivedFromXnatObjectProperty(xnatObjectProperty)
                .providesValueForCommandInput(stringInputName)
                .build();

        final String outputHandlerName = "output-handler-name2";
        final String outputHandlerLabel = "a_label";
        final CommandWrapperOutput outputHandler = CommandWrapperOutput.builder()
                .name(outputHandlerName)
                .commandOutputName(commandOutputName)
                .targetName(externalInputName)
                .type("Resource")
                .label(outputHandlerLabel)
                .build();

        final String commandWrapperName = "altwrappername";
        final String commandWrapperDesc = "alt wrapper description";
        final CommandWrapper commandWrapper = CommandWrapper.builder()
                .name(commandWrapperName)
                .description(commandWrapperDesc)
                .addExternalInput(externalInput)
                .addDerivedInput(derivedInput)
                .addOutputHandler(outputHandler)
                .build();

        CommandEntity created = commandEntityService.create(COMMAND_ENTITY);
        TestingUtils.commitTransaction();

        //update with addl input and output and mount and wrapper
        Command cmd = COMMAND.toBuilder()
                .addInput(stringInput)
                .addOutput(commandOutput)
                .addMount(mountIn)
                .addCommandWrapper(commandWrapper)
                .build();

        created.update(cmd);
        commandEntityService.update(created);
        TestingUtils.commitTransaction();
        CommandEntity retrieved = commandEntityService.retrieve(created.getId());
        assertThat(retrieved.getInputs(), Matchers.<CommandInputEntity>hasSize(COMMAND.inputs().size() + 1));
        assertThat(retrieved.getOutputs(), Matchers.<CommandOutputEntity>hasSize(COMMAND.outputs().size() + 1));
        assertThat(retrieved.getMounts(), Matchers.<CommandMountEntity>hasSize(COMMAND.mounts().size() + 1));
        assertThat(retrieved.getCommandWrapperEntities(),
                Matchers.<CommandWrapperEntity>hasSize(COMMAND.xnatCommandWrappers().size() + 1));

        //remove them
        retrieved.update(COMMAND);
        commandEntityService.update(retrieved);
        TestingUtils.commitTransaction();
        CommandEntity retrievedAnew = commandEntityService.retrieve(created.getId());
        assertThat(retrievedAnew.getInputs(), Matchers.<CommandInputEntity>hasSize(COMMAND.inputs().size()));
        assertThat(retrievedAnew.getOutputs(), Matchers.<CommandOutputEntity>hasSize(COMMAND.outputs().size()));
        assertThat(retrievedAnew.getMounts(), Matchers.<CommandMountEntity>hasSize(COMMAND.mounts().size()));
        assertThat(retrievedAnew.getCommandWrapperEntities(),
                Matchers.<CommandWrapperEntity>hasSize(COMMAND.xnatCommandWrappers().size()));
    }

    @Test
    @DirtiesContext
    public void testRemoveEntitiesFromWrapper() throws Exception {
        final String outputMountName = "out";
        final CommandMount mountIn = CommandMount.create("in2", false, "/input2");

        final String stringInputName = "foo2";
        final CommandInput stringInput = CommandInput.builder()
                .name(stringInputName)
                .description("A foo that bars")
                .required(false)
                .defaultValue("bar")
                .commandLineFlag("--flag")
                .commandLineSeparator("=")
                .build();
        final String stringInputName2 = "foo2";
        final CommandInput stringInput2 = CommandInput.builder()
                .name(stringInputName2)
                .description("A foo that bars")
                .required(false)
                .defaultValue("bar")
                .commandLineFlag("--flag")
                .commandLineSeparator("=")
                .build();

        final String commandOutputName = "the_output2";
        final CommandOutput commandOutput = CommandOutput.builder()
                .name(commandOutputName)
                .description("It's the output")
                .mount(outputMountName)
                .path("relative/path/to/dir")
                .build();
        final String commandOutputName2 = "the_output_alt";
        final CommandOutput commandOutput2 = CommandOutput.builder()
                .name(commandOutputName2)
                .description("It's the output")
                .mount(outputMountName)
                .path("relative/path/to/dir")
                .build();

        final String externalInputName = "session";
        final CommandWrapperExternalInput externalInput = CommandWrapperExternalInput.builder()
                .name(externalInputName)
                .type("Session")
                .build();

        final String externalInputName2 = "project";
        final CommandWrapperExternalInput externalInput2 = CommandWrapperExternalInput.builder()
                .name(externalInputName2)
                .type("Project")
                .build();

        final String derivedInputName = "label";
        final String xnatObjectProperty = "label";
        final CommandWrapperDerivedInput derivedInput = CommandWrapperDerivedInput.builder()
                .name(derivedInputName)
                .type("string")
                .derivedFromWrapperInput(externalInputName)
                .derivedFromXnatObjectProperty(xnatObjectProperty)
                .providesValueForCommandInput(stringInputName)
                .build();
        final String derivedInputName2 = "label2";
        final CommandWrapperDerivedInput derivedInput2 = CommandWrapperDerivedInput.builder()
                .name(derivedInputName2)
                .type("string")
                .derivedFromWrapperInput(externalInputName)
                .derivedFromXnatObjectProperty(xnatObjectProperty)
                .providesValueForCommandInput(stringInputName2)
                .build();

        final String outputHandlerName = "output-handler-name2";
        final String outputHandlerLabel = "a_label";
        final CommandWrapperOutput outputHandler = CommandWrapperOutput.builder()
                .name(outputHandlerName)
                .commandOutputName(commandOutputName)
                .targetName(externalInputName)
                .type("Resource")
                .label(outputHandlerLabel)
                .build();

        final String outputHandlerName2 = "output-handler-name-alt";
        final String outputHandlerLabel2 = "a_label_alt";
        final CommandWrapperOutput outputHandler2 = CommandWrapperOutput.builder()
                .name(outputHandlerName2)
                .commandOutputName(commandOutputName2)
                .targetName(externalInputName2)
                .type("Resource")
                .label(outputHandlerLabel2)
                .build();

        //test add/remove from wrapper
        String newWrapperName = "new-wrapper";
        CommandWrapper newWrapper = CommandWrapper.builder()
                .name(newWrapperName)
                .description("desc")
                .addExternalInput(externalInput)
                .addExternalInput(externalInput2)
                .addDerivedInput(derivedInput)
                .addDerivedInput(derivedInput2)
                .addOutputHandler(outputHandler)
                .addOutputHandler(outputHandler2)
                .build();

        CommandEntity created = commandEntityService.create(COMMAND_ENTITY);
        TestingUtils.commitTransaction();

        //update with new wrapper
        Command cmd = COMMAND.toBuilder()
                .addCommandWrapper(newWrapper)
                .build();
        created.update(cmd);
        commandEntityService.update(created);
        TestingUtils.commitTransaction();
        CommandEntity retrieved = commandEntityService.retrieve(created.getId());
        CommandWrapperEntity commandWrapperEntityRetrieved = null;
        for (CommandWrapperEntity entity : retrieved.getCommandWrapperEntities()) {
            if (entity.getName().equals(newWrapperName)) {
                commandWrapperEntityRetrieved = entity;
                break;
            }
        }
        assertThat(commandWrapperEntityRetrieved, not(nullValue()));

        assertThat(commandWrapperEntityRetrieved.getExternalInputs(),
                Matchers.<CommandWrapperExternalInputEntity>hasSize(newWrapper.externalInputs().size()));
        assertThat(commandWrapperEntityRetrieved.getDerivedInputs(),
                Matchers.<CommandWrapperDerivedInputEntity>hasSize(newWrapper.derivedInputs().size()));
        assertThat(commandWrapperEntityRetrieved.getOutputHandlers(),
                Matchers.<CommandWrapperOutputEntity>hasSize(newWrapper.outputHandlers().size()));

        // And remove (no way to directly remove, mimicking removal through json)
        CommandWrapper newWrapperMod = CommandWrapper.builder()
                .name(newWrapperName)
                .description("desc")
                .build();
        commandWrapperEntityRetrieved.update(newWrapperMod);
        commandEntityService.update(commandWrapperEntityRetrieved);
        CommandWrapperEntity commandWrapperEntityRetrievedAnew =
                commandEntityService.retrieveWrapper(commandWrapperEntityRetrieved.getId());

        assertThat(commandWrapperEntityRetrievedAnew.getExternalInputs(),
                Matchers.<CommandWrapperExternalInputEntity>hasSize(newWrapperMod.externalInputs().size()));
        assertThat(commandWrapperEntityRetrievedAnew.getDerivedInputs(),
                Matchers.<CommandWrapperDerivedInputEntity>hasSize(newWrapperMod.derivedInputs().size()));
        assertThat(commandWrapperEntityRetrievedAnew.getOutputHandlers(),
                Matchers.<CommandWrapperOutputEntity>hasSize(newWrapperMod.outputHandlers().size()));
    }


    @Test
    @DirtiesContext
    public void testGetCommandsByImage() throws Exception {
        final String fooImage = "xnat/foo:1.2.3";
        final String barImage = "xnat/bar:4.5.6";
        final Command fooImageCommand1 = Command.builder()
                .image(fooImage)
                .name("soahs")
                .version("0")
                .build();
        final Command fooImageCommand2 = Command.builder()
                .image(fooImage)
                .name("asuyfo")
                .version("0")
                .build();
        final Command barImageCommand = Command.builder()
                .image(barImage)
                .name("dosfa")
                .version("0")
                .build();

        final CommandEntity fooImageCommandEntity1 = commandEntityService.create(CommandEntity.fromPojo(fooImageCommand1));
        final CommandEntity fooImageCommandEntity2 = commandEntityService.create(CommandEntity.fromPojo(fooImageCommand2));
        final CommandEntity barImageCommandEntity = commandEntityService.create(CommandEntity.fromPojo(barImageCommand));

        final List<CommandEntity> fooImageCommandsRetrieved = commandEntityService.getByImage(fooImage);
        assertThat(fooImageCommandsRetrieved, hasSize(2));
        assertThat(fooImageCommandsRetrieved, contains(fooImageCommandEntity1, fooImageCommandEntity2));
        assertThat(fooImageCommandsRetrieved, not(contains(barImageCommandEntity)));

        final List<CommandEntity> barImageCommandsRetrieved = commandEntityService.getByImage(barImage);
        assertThat(barImageCommandsRetrieved, hasSize(1));
        assertThat(barImageCommandsRetrieved, not(contains(fooImageCommandEntity1, fooImageCommandEntity2)));
        assertThat(barImageCommandsRetrieved, contains(barImageCommandEntity));
    }

    @Test
    @DirtiesContext
    public void testCreateEcatHeaderDump() throws Exception {
        // A User was attempting to create the command in this resource.
        // Spring didn't tell us why. See CS-70.
        final String dir = Paths.get(ClassLoader.getSystemResource("ecatHeaderDump").toURI()).toString().replace("%20", " ");
        final String commandJsonFile = dir + "/command.json";
        final Command ecatHeaderDump = mapper.readValue(new File(commandJsonFile), Command.class);
        commandEntityService.create(CommandEntity.fromPojo(ecatHeaderDump));
    }

    @Test
    @DirtiesContext
    public void testCreateSetupCommand() throws Exception {
        final Command setupCommand = Command.builder()
                .name("setup")
                .type("docker-setup")
                .image("a-setup-image")
                .build();
        final List<String> errors = setupCommand.validate();
        assertThat(errors, is(Matchers.<String>emptyIterable()));
        final CommandEntity createdSetupCommandEntity = commandEntityService.create(CommandEntity.fromPojo(setupCommand));
    }

    @Test
    @DirtiesContext
    public void testCreateWrapupCommand() throws Exception {
        final Command wrapup = Command.builder()
                .name("wrapup")
                .type("docker-wrapup")
                .image("a-wrapup-image")
                .build();
        final List<String> errors = wrapup.validate();
        assertThat(errors, is(Matchers.<String>emptyIterable()));
        final CommandEntity createdWrapupCommandEntity = commandEntityService.create(CommandEntity.fromPojo(wrapup));
    }

    @Test
    @DirtiesContext
    public void testLongCommandLine() throws Exception {
        final String alphanumeric = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
        final SecureRandom rnd = new SecureRandom();
        final int stringSize = 2048;

        final StringBuilder sb = new StringBuilder( stringSize );
        for( int i = 0; i < stringSize; i++ ) {
            sb.append(alphanumeric.charAt(rnd.nextInt(alphanumeric.length())));
        }
        final String longString = sb.toString();

        final CommandEntity command = commandEntityService.create(
                CommandEntity.fromPojo(Command.builder()
                        .name("long")
                        .image("foo")
                        .commandLine(longString)
                        .build())
        );

        TestingUtils.commitTransaction();

        assertThat(commandEntityService.get(command.getId()).getCommandLine(), is(longString));
    }
}
