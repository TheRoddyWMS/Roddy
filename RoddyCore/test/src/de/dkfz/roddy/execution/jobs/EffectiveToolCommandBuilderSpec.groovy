package de.dkfz.roddy.execution.jobs

import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.ExecutionContextLevel
import de.dkfz.roddy.core.JobExecutionEnvironment
import de.dkfz.roddy.execution.Code
import de.dkfz.roddy.execution.Command
import de.dkfz.roddy.execution.Executable
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Paths

import static de.dkfz.roddy.execution.EscapableString.*

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
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(ctx)

        when:
        Optional<ToolCommand> result = builder.build(new ToolIdCommand(toolId))

        then:
        result.isPresent()
        1 * ctx.getToolCommand(toolId) >> someToolCommand
        result.get().command instanceof Command
        (result.get().command as Command).executablePath.toString() == "someWrapper"
        (result.get().command as Command).toCommandSegmentList() == [u("someWrapper")]
    }

    def "simple command is replaced by wrapper call"() {
        given:
        ExecutionContext ctx = Mock(ExecutionContext.class)
        ctx.wrapInCommand >> someWrapper
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(ctx)

        when:
        Optional<ToolCommand> result = builder.build(someToolCommand)

        then:
        result.isPresent()
        result.get().command instanceof Command
        (result.get().command as Command).executablePath.toString() == "someWrapper"
        (result.get().command as Command).toCommandSegmentList() == [u("someWrapper")]
    }

    def "unknown tool command but not QUERY_STATUS mode throws"(level) {
        given:
        ExecutionContext ctx = Mock(ExecutionContext)
        ctx.getExecutionContextLevel() >> level
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(ctx)

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

    def "unknown tool command throws if submission is prohibited"(level) {
        given:
        ExecutionContext ctx = Mock(ExecutionContext)
        ctx.getExecutionContextLevel() >> level
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(ctx)

        when:
        builder.build(new UnknownToolCommand("unknown"))

        then:
        final IllegalArgumentException exception = thrown()

        where:
        level                           | _
        ExecutionContextLevel.CLEANUP   | _
        ExecutionContextLevel.RERUN     | _
        ExecutionContextLevel.RUN       | _
        ExecutionContextLevel.ABORTED   | _
    }

    def "sg without apptainer"() {
        given:
        ExecutionContext ctx = Mock(ExecutionContext)
        ctx.jobExecutionEnvironment >> JobExecutionEnvironment.bash
        ctx.outputGroupString >> "someGroup"
        ctx.wrapInCommand >> someWrapper
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(ctx)

        when:
        Optional<ToolCommand> result = builder.build(someToolCommand)

        then:
        result.isPresent()
        result.get().command instanceof Command
        (result.get().command as Command).toCommandSegmentList() == [u("someWrapper")]
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
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(ctx)

        when:
        Optional<ToolCommand> result = builder.build(someToolCommand)

        then:
        result.isPresent()
        result.get().command instanceof Code
        // It is important for DefaultPlugin < 1.3 that `sgWasCalled=true` and for < 1.2.2-4 that
        // `newGrpIsCalled=true`. Newer versions of the plugin compare the actual primary group
        // against the desired target group.
        forBash((result.get().command as Code).toEscapableString(true)) ==
                "#!/bin/bash\n" +
                "sg someGroup -c apptainer\\ exec\\ --env\\ sgWasCalled\\=true\\ --env\\ newGrpIsCalled\\=true\\ -W\\ /tmp\\ someImage\\ someWrapper\n"
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
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(ctx)

        when:
        Optional<ToolCommand> result = builder.build(toolCommand)

        then:
        result.isPresent()
        result.get().command instanceof Code
        // It is OK to set `sgWasCalled=false` because the primary group is not being changed.
        forBash(result.get().command.toEscapableString()) ==
                "#!/bin/bash\n" +
                "apptainer exec --env sgWasCalled=true --env newGrpIsCalled=true -W /tmp someImage someWrapper"

        where:
        toolCommand                                                                           | _
        someToolCommand                                                                       | _
        new ToolCommand("toolId", new Executable(Paths.get("test.sh")), Paths.get("test.sh")) | _
    }

    def "apptainer with code should fail with context error but return Optional.empty()"() {
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
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(ctx)

        when:
        Optional<ToolCommand> result = builder.build(new ToolCommand("toolId",
                                                                     new Code("echo"),
                                                                     Paths.get("localScript")))
        then:
        result == Optional.empty()
        1 * ctx.addError(_)
    }

    def "not implemented for ToolCommands with Command objects"() {
        given:
        ExecutionContext mock = Mock(ExecutionContext.class)
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(mock)

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
        EffectiveToolCommandBuilder builder = EffectiveToolCommandBuilder.from(mock)
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
