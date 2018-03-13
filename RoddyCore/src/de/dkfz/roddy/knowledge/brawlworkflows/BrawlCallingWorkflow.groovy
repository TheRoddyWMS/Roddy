package de.dkfz.roddy.knowledge.brawlworkflows

import de.dkfz.roddy.Roddy
import de.dkfz.roddy.config.ConfigurationError
import de.dkfz.roddy.core.ExecutionContext
import de.dkfz.roddy.core.Workflow
import de.dkfz.roddy.plugins.LibrariesFactory
import groovy.transform.CompileStatic

/**
 * Brawl workflows
 */
@CompileStatic
class BrawlCallingWorkflow extends Workflow {
    @Override
    boolean execute(ExecutionContext context) throws ConfigurationError {

        String brawlID = context.getConfigurationValues().get("activeBrawlWorkflow")
        if (!brawlID)
            return false

        File brawlFile = context.configuration.getSourceBrawlWorkflow(brawlID);
        List<String> brawlCode = Roddy.getLocalFileSystemAccessProvider().loadTextFile(brawlFile) as List<String>
        brawlCode.add(0, "def wf = BrawlWorkflow.create context, {")
        brawlCode.add(0, "import de.dkfz.roddy.knowledge.brawlworkflows.*")
        brawlCode << "}"
        File temporaryScript = File.createTempFile("roddy", "brawlTemp")
        temporaryScript << brawlCode.join("\n")

        try {
            def cl = LibrariesFactory.instance.getGroovyClassLoader()
            def se = new GroovyScriptEngine(temporaryScript.getAbsolutePath(), cl)
            Binding bind = new Binding()
            bind.setVariable("context", context)
            def obj = se.run(temporaryScript.absolutePath, bind)

            return true
        } finally {
            temporaryScript.delete()
        }
        return false
    }
}
