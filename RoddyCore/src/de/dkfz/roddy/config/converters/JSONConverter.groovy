/*
 * Copyright (c) 2017 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters

import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.ConfigurationValue
import de.dkfz.roddy.core.ExecutionContext
import groovy.json.JsonSlurper
import groovy.transform.CompileStatic
import groovy.transform.TypeCheckingMode
import groovy.xml.MarkupBuilder
import sun.reflect.generics.reflectiveObjects.NotImplementedException

/**
 * In exception to our other Groovy classes, this class needs to be CompileDynamic!
 * We use MarkupBuilder closures, where you cannot attach the CompileDynamic directive.
 *
 * Created by heinold on 30.01.17.
 */
class JSONConverter extends ConfigurationConverter {

    @CompileStatic
    @Override
    String convert(ExecutionContext context, Configuration configuration) {
        throw new NotImplementedException()
    }

    @CompileStatic
    @Override
    StringBuilder convertConfigurationValue(ConfigurationValue cv, ExecutionContext context) {
        throw new NotImplementedException()
    }

    @CompileStatic
    @Override
    String convertToXML(File file) {
        convertJsonString(file.text)
    }

    @CompileStatic
    private String readAttribute(String attribute, def nc) { nc?.getAt(attribute) ?: "" }

    @CompileStatic
    private String idOf(def nc) { return readAttribute("id", nc) }

    @CompileStatic
    private String nameOf(def nc) { return readAttribute("name", nc) }

    @CompileStatic
    private String valueOf(def nc) { return readAttribute("value", nc) }

    @CompileStatic
    private String descriptionOf(def nc) { return readAttribute("description", nc) }

    @CompileStatic
    private String classOf(def nc) { return readAttribute("class", nc) }

    @CompileStatic
    private String typeOf(def nc) { return readAttribute("type", nc) }

    @CompileStatic
    private String typeofOf(def nc) { return readAttribute("typeof", nc) }

    @CompileStatic
    private String scriptparameterOf(def nc) { return readAttribute("scriptparameter", nc) }

    @CompileStatic
    private String sizeOf(def nc) { return readAttribute("size", nc) }

    @CompileStatic
    private String memoryOf(def nc) { return readAttribute("memory", nc) }

    @CompileStatic
    private String coresOf(def nc) { return readAttribute("cores", nc) }

    @CompileStatic
    private String nodesOf(def nc) { return readAttribute("nodes", nc) }

    @CompileStatic
    private String walltimeOf(def nc) { return readAttribute("walltime", nc) }

    @CompileStatic(TypeCheckingMode.SKIP)
    String convertJsonString(String json) {
        /**
         Remove comments first, they are not allowed in JSON and will produce errors.

         See also a comment from Douglas Crockford:

         I removed comments from JSON because I saw people were using them to hold
         parsing directives, a practice which would have destroyed interoperability.
         I know that the lack of comments makes some people sad, but it shouldn't.

         Suppose you are using JSON to keep configuration files, which you would
         like to annotate. Go ahead and insert all the comments you like. Then
         pipe it through JSMin before handing it to your JSON parser.

         to be found here: https://plus.google.com/+DouglasCrockfordEsq/posts/RK8qyGVaGSr
         */

        json = json.readLines().findAll { !(it.startsWith("//") || it.startsWith("#")) }.join("\n")
        def content = new JsonSlurper().parseText(json)

        def writer = new StringWriter()
        def xml = new MarkupBuilder(writer)
        xml.omitEmptyAttributes = true
        xml.omitNullAttributes = true

        def cfg = content["configuration"]
        xml.configuration(
                id: idOf(cfg),
                description: descriptionOf(cfg),
                configurationType: readAttribute("configurationType", cfg),
                class: classOf(cfg),
                workflowClass: readAttribute("workflowClass", cfg),
                runtimeServiceClass: readAttribute("runtimeServiceClass", cfg),
                imports: readAttribute("imports", cfg),
                listOfUsedTools: readAttribute("listOfUsedTools", cfg),
                usedToolFolders: readAttribute("usedToolFolders", cfg),
        ) {
            processAnalyses(xml, cfg)
            processCValues(xml, cfg)
            processTools(xml, cfg)
            processFilenames(xml, cfg)
        }

        writer.toString()
    }

    Closure<MarkupBuilder> processAnalyses = { MarkupBuilder builder, def cfg ->
        if (!(cfg["analyses"]))
            return
        builder.analyses {
            cfg["analyses"]?.each { _analysis ->
                analysis(
                        id: idOf(_analysis),
                        configuration: readAttribute("configuration", _analysis),
                        useplugin: readAttribute("useplugin", _analysis)
                )
            }
        }
    }

    Closure<MarkupBuilder> processCValues = { MarkupBuilder builder, def cfg ->
        if (!cfg["configurationvalues"]) return
        builder.configurationvalues {
            cfg["configurationvalues"]?.each { _cvalue ->
                cvalue(name: nameOf(_cvalue), value: valueOf(_cvalue), description: descriptionOf(_cvalue), type: typeOf(_cvalue))
            }
        }
    }

    Closure<MarkupBuilder> processTools = { MarkupBuilder builder, def cfg ->
        if (!cfg["processingtools"]) return
        builder.processingtools {
            cfg["processingtools"]?.each { _tool ->
                tool(name: nameOf(_tool),
                        value: valueOf(_tool),
                        basepath: readAttribute("basepath", _tool)) {
                    processResourcesets(builder, _tool)
                    _tool["input"].each { _input ->
                        input(type: typeOf(_input),
                                typeof: typeofOf(_input),
                                scriptparameter: scriptparameterOf(_input)
                        )
                    }
                    _tool["output"].each { _output ->
                        output(type: typeOf(_output),
                                typeof: typeofOf(_output),
                                scriptparameter: scriptparameterOf(_output),
                                filename: readAttribute("filename", _output)
                        )
                    }
                }
            }
        }
    }

    Closure<MarkupBuilder> processResourcesets = { MarkupBuilder builder, def _tool ->
        if (_tool["resourcesets"]) {
            builder.resourcesets {
                _tool["resourcesets"]?.each { _rset ->
                    rset(size: sizeOf(_rset),
                            memory: memoryOf(_rset),
                            cores: coresOf(_rset),
                            nodes: nodesOf(_rset),
                            walltime: walltimeOf(_rset)
                    ) {}
                }
            }
        }
    }

    Closure<MarkupBuilder> processFilenames = { MarkupBuilder builder, def cfg ->
        if (!(cfg["filenames"]))
            return
        builder.filenames(package: readAttribute("package", cfg["filenames"]), filestagesbase: readAttribute("filestagesbase", cfg["filenames"])) {
            cfg["filenames"]?.getAt("patterns")?.each { _filename ->
                filename(
                        class: classOf(_filename),
                        derivedFrom: readAttribute("derivedFrom", _filename),
                        onMethod: readAttribute("onMethod", _filename),
                        onScriptParameter: readAttribute("onScriptParameter", _filename),
                        onTool: readAttribute("onTool", _filename),
                        selectiontag: readAttribute("selectiontag", _filename),
                        pattern: readAttribute("pattern", _filename),
                )
            }
        }
    }
}
