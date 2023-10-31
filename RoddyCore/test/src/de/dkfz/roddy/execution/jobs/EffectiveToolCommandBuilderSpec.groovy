package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.JobExecutionEnvironment
import de.dkfz.roddy.execution.Code
import de.dkfz.roddy.execution.Executable
import de.dkfz.roddy.execution.Command
import spock.lang.Shared
import spock.lang.Specification
import sun.reflect.generics.reflectiveObjects.NotImplementedException

import java.nio.file.Paths

class EffectiveToolCommandBuilderSpec extends Specification {

    @Shared
    ToolCommand someToolCommand = new ToolCommand("test",
                                                  new Executable(Paths.get("test.sh")),
                                                  Paths.get("test.sh"))

    @Shared
    Command someWrapper = new Command(new Executable(Paths.get("someWrapper")), [])

    def "simple toolId command"() {
        given:
        String toolId = "toolId"
        ExecutionContext ctx = Mock(ExecutionContext.class)
        ctx.wrapInCommand >> someWrapper
        ctx.getToolCommand(_) >> someToolCommand
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(ctx)

        when:
        Optional<ToolCommand> result = builder.build(new ToolIdCommand(toolId))

        then:
        result.isPresent()
        1 * ctx.getToolCommand(toolId) >> someToolCommand
        result.get().command instanceof Command
        result.get().command.executablePath.toString() == "someWrapper"
        result.get().command.toList() == ["someWrapper"]
    }

    def "simple command is replaced by wrapper call"() {
        given:
        ExecutionContext ctx = Mock(ExecutionContext.class)
        ctx.wrapInCommand >> someWrapper
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(ctx)

        when:
        Optional<ToolCommand> result = builder.build(someToolCommand)

        then:
        result.isPresent()
        result.get().command instanceof Command
        result.get().command.executablePath.toString() == "someWrapper"
        result.get().command.toList() == ["someWrapper"]
    }

    def "unknown tool command but not QUERY_STATUS mode throws"(level) {
        given:
        ExecutionContext ctx = Mock(ExecutionContext)
        ctx.getExecutionContextLevel() >> level
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(ctx)

        when:
        builder.build(new UnknownToolCommand("unknown"))

        then:
        noExceptionThrown()

        where:
        level                                | _
        ExecutionContextLevel.QUERY_STATUS   | _
        ExecutionContextLevel.TESTRERUN      | _
        ExecutionContextLevel.READOUT        | _
        ExecutionContextLevel.CREATETESTDATA | _
        ExecutionContextLevel.UNSET          | _
    }

    def "unknown tool command does not throw if submission is prohibited"(level) {
        given:
        ExecutionContext ctx = Mock(ExecutionContext)
        ctx.getExecutionContextLevel() >> level
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(ctx)

        when:
        builder.build(new UnknownToolCommand("unknown"))

        then:
        final IllegalArgumentException exception = thrown()

        where:
        level                           | _
        ExecutionContextLevel.CLEANUP   | _
        ExecutionContextLevel.RERUN     | _
        ExecutionContextLevel.RUN       | _
        ExecutionContextLevel.ABORTED   | _     // But see comment on ABORTED
    }

    def "sg without apptainer"() {
        given:
        ExecutionContext ctx = Mock(ExecutionContext)
        ctx.jobExecutionEnvironment >> JobExecutionEnvironment.bash
        ctx.outputGroupString >> "someGroup"
        ctx.wrapInCommand >> someWrapper
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(ctx)

        when:
        Optional<ToolCommand> result = builder.build(someToolCommand)

        then:
        result.present
        result.get().command instanceof Command
        result.get().command.toList() == ["someWrapper"]
    }

    def "sg with apptainer"() {
        given:
        ExecutionContext ctx = Mock(ExecutionContext)
        ctx.jobExecutionEnvironment >> JobExecutionEnvironment.apptainer
        ctx.outputGroupString >> "someGroup"
        ctx.userContainerEngineArguments >> []
        ctx.containerEnginePath >> Paths.get("apptainer")
        ctx.roddyContainerCopyVariables >> []
        ctx.containerImage >> "someImage"
        ctx.roddyMounts >> []
        ctx.userContainerMounts >> []
        ctx.outputDirectory >> new File("/tmp")
        ctx.wrapInCommand >> someWrapper
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(ctx)

        when:
        Optional<ToolCommand> result = builder.build(someToolCommand)

        then:
        result.present
        result.get().command instanceof Command
        result.get().command.toList() ==
            ["sg", "someGroup", "-c", "apptainer\\ exec\\ --env\\ sgWasCalled=true\\ -W\\ /tmp\\ someImage\\ someWrapper"]
    }

    def "no sg with apptainer()"(toolCommand) {
        given:
        ExecutionContext ctx = Mock(ExecutionContext)
        ctx.jobExecutionEnvironment >> JobExecutionEnvironment.apptainer
        ctx.userContainerEngineArguments >> []
        ctx.containerEnginePath >> Paths.get("apptainer")
        ctx.roddyContainerCopyVariables >> []
        ctx.containerImage >> "someImage"
        ctx.roddyMounts >> []
        ctx.userContainerMounts >> []
        ctx.outputDirectory >> new File("/tmp")
        ctx.wrapInCommand >> someWrapper
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(ctx)

        when:
        Optional<ToolCommand> result = builder.build(toolCommand)

        then:
        result.present
        result.get().command instanceof Command
        result.get().command.toList() ==
        ["apptainer", "exec", "--env", "sgWasCalled=true", "-W", "/tmp", "someImage", "someWrapper"]

        where:
        toolCommand                                                                           | _
        someToolCommand                                                                       | _
        new ToolCommand("toolId", new Executable(Paths.get("test.sh")), Paths.get("test.sh")) | _
    }

    def "apptainer with code should fail"() {
        given:
        ExecutionContext ctx = Mock(ExecutionContext)
        ctx.jobExecutionEnvironment >> JobExecutionEnvironment.apptainer
        ctx.userContainerEngineArguments >> []
        ctx.containerEnginePath >> Paths.get("apptainer")
        ctx.roddyContainerCopyVariables >> []
        ctx.containerImage >> "someImage"
        ctx.roddyMounts >> []
        ctx.userContainerMounts >> []
        ctx.outputDirectory >> new File("/tmp")
        ctx.wrapInCommand >> someWrapper
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(ctx)

        when:
        builder.build(new ToolCommand("toolId",
                                      new Code("echo"),
                                      Paths.get("localScript")))

        then:
        final UnsupportedOperationException exception = thrown()
    }

    def "not implemented for ToolCommands with Command objects"() {
        given:
        ExecutionContext mock = Mock(ExecutionContext.class)
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(mock)

        when:
        builder.build(
                new ToolCommand("test",
                                new Command(new Executable(Paths.get("test.sh")), []),
                                Paths.get("test.sh")))
        then:
        final UnsupportedOperationException exception = thrown()
    }

    def "code commands should not be wrapped, but communicated as is to BatchEuphoria"() {
        given:
        ExecutionContext mock = Mock(ExecutionContext.class)
        EffectiveToolCommandBuilder builder = new EffectiveToolCommandBuilder(mock)
        ToolCommand codeToolCommand = new ToolCommand("test",
                                                      new Code("echo"),
                                                      Paths.get("test.sh"))

        when:
        Optional<ToolCommand> result = builder.build(codeToolCommand)

        then:
        result.isPresent()
        result.get() == codeToolCommand
    }

}