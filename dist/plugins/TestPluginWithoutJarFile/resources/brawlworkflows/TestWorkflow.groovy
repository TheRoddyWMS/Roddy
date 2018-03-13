/**
 * Test workflow for the new Brawl Workflow DSL
 */
// Configuration
String variable = "abc"

// Explicit workflow ?
explicit {
    def files = getSourceFile("/tmp")
    def a = call
}

// Or implicit like Snakemake? Later maybe

// Tool / Rule section
rule "ToolA", {
    input "aClass", "parameter", "abc"
    output "aClass", "parameter", "abc"
    shell """
                #!/bin/bash


            """
}
