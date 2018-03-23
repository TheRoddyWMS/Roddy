/**
 * Test workflow for the new Brawl Workflow DSL
 */
// Configuration
String variable = "abc"

// Explicit workflow. Implicit might follow later
explicit {
    def file = getSourceFile("/tmp", "TextFile")
    def a = run "ToolA", file
}

// Tool / Rule section
rule "ToolA", {
    input "TextFile", "parameterA"
    output "aClass", "parameterB", "/tmp/someoutputfile"
    shell """
                #!/bin/bash
                echo "\$parameterA"
                echo "\$parameterB"
                touch \$parameterB

            """
}
