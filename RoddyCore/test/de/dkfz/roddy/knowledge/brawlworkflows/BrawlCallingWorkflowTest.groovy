package de.dkfz.roddy.knowledge.brawlworkflows

import de.dkfz.roddy.RunMode
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.config.ToolEntry
import de.dkfz.roddy.core.MockupExecutionContextBuilder
import de.dkfz.roddy.execution.io.ExecutionService
import de.dkfz.roddy.execution.io.LocalExecutionService
import groovy.transform.CompileStatic
import org.junit.Test

import static org.junit.Assert.*

@CompileStatic
class BrawlCallingWorkflowTest {

    @Test
    void testExecute() {
        File temp = File.createTempFile("RODDYTEST", ".brawlworkflow")

        temp << '''
                /**
                 * Test workflow for the new Brawl Workflow DSL
                 */
                // Configuration
                String variable = "abc"
                
                // Tool / Rule section
                rule "ToolA", {
                    output "TextFile", "OUT_FILE", "/tmp/abc"
                    shell """
                        #!/bin/bash
                        echo "{OUT_FILE}"               
                        """
                }
                
                // Tool / Rule section
                rule "ToolB", {
                    input "TextFile", "IN_FILE"
                    output "TextFile", "OUT_FILE", "/tmp/abc"
                    shell """
                        #!/bin/bash
                        echo "\\${OUT_FILE}"
                        """
                }
                
                // Explicit workflow ?
                explicit {
                    def file = getSourceFileUsingTool("ToolA")
                    def outfile = call "ToolB", file
                }
                
                // Or implicit like Snakemake? Later maybe
            '''
        Configuration cfg = new Configuration() {
            @Override
            File getSourceBrawlWorkflow(String brawlName) {
                return temp
            }
        }
        def entry = new ToolEntry("wrapinScript", "brawlWorkflow", "")
        entry.inlineScript = """source \$PARAMETER_FILE; source \$WRAPPED_SCRIPT"""
        cfg.tools << entry
        ExecutionService.initializeService(LocalExecutionService, RunMode.CLI)
        cfg.configurationValues << new ConfigurationValue("activeBrawlWorkflow", temp.absolutePath)
        def context = MockupExecutionContextBuilder.createSimpleContext(BrawlCallingWorkflowTest, cfg)
        try {

            def wf = new BrawlCallingWorkflow()
            wf.execute(context)
        } finally {
            temp.delete()
        }
    }
}