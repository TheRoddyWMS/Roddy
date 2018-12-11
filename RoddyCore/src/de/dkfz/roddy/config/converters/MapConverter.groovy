/*
 * Copyright (c) 2018 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.converters

import groovy.transform.CompileStatic
import groovy.xml.MarkupBuilder

/**
 * In exception to our other Groovy classes, this class needs to be CompileDynamic!
 * We use MarkupBuilder closures, where you cannot attach the CompileDynamic directive.
 *
 * Created by heinold on 30.01.17.
 */
abstract class MapConverter extends ConfigurationConverter {

    protected String readAttribute(def attribute, def nc) {
        nc?.getAt(attribute) ?: ""
    }

    @CompileStatic
    protected String idOf(def nc) { return readAttribute("id", nc) }

    @CompileStatic
    protected String nameOf(def nc) { return readAttribute("name", nc) }

    @CompileStatic
    protected String valueOf(def nc) { return readAttribute("value", nc) }

    @CompileStatic
    protected String descriptionOf(def nc) { return readAttribute("description", nc) }

    @CompileStatic
    protected String classOf(def nc) { return readAttribute("class", nc) }

    @CompileStatic
    protected String typeOf(def nc) { return readAttribute("type", nc) }

    @CompileStatic
    protected String typeofOf(def nc) { return readAttribute("typeof", nc) }

    @CompileStatic
    protected String scriptparameterOf(def nc) { return readAttribute("scriptparameter", nc) }

    @CompileStatic
    protected String sizeOf(def nc) { return readAttribute("size", nc) }

    @CompileStatic
    protected String memoryOf(def nc) { return readAttribute("memory", nc) }

    @CompileStatic
    protected String coresOf(def nc) { return readAttribute("cores", nc) }

    @CompileStatic
    protected String nodesOf(def nc) { return readAttribute("nodes", nc) }

    @CompileStatic
    protected String walltimeOf(def nc) { return readAttribute("walltime", nc) }

    protected String convertMap(def content) {

        def writer = new StringWriter()
        def builder = new MarkupBuilder(writer)
        builder.omitEmptyAttributes = true
        builder.omitNullAttributes = true

        def cfg = content["configuration"]
        builder.configuration(
                name: nameOf(cfg),
                description: descriptionOf(cfg),
                usedresourcessize: readAttribute("usedresourcessize", cfg),
                configurationType: readAttribute("configurationType", cfg),
                class: classOf(cfg),
                workflowClass: readAttribute("workflowClass", cfg),
                runtimeServiceClass: readAttribute("runtimeServiceClass", cfg),
                imports: readAttribute("imports", cfg),
                listOfUsedTools: readAttribute("listOfUsedTools", cfg),
                usedToolFolders: readAttribute("usedToolFolders", cfg),
        ) {
            processConfigurationContent(builder, cfg)
        }

        writer.toString()
    }

    protected Closure<MarkupBuilder> processConfigurationContent = { MarkupBuilder builder, def cfg ->
        processAnalyses(builder, cfg["availableanalyses"])
        processCValues(builder, cfg["configurationvalues"])
        processTools(builder, cfg["processingtools"])
        processFilenames(builder, cfg["filenames"])
        processSubconfigurations(builder, cfg["subconfigurations"])
    }

    protected Closure<MarkupBuilder> processAnalyses = { MarkupBuilder builder, def analyses ->
        if (!(analyses)) return
        builder.availableanalyses {
            analyses?.each { _analysis ->
                analysis(
                        id: idOf(_analysis),
                        configuration: readAttribute("configuration", _analysis),
                        useplugin: readAttribute("useplugin", _analysis)
                )
            }
        }
    }

    protected Closure<MarkupBuilder> processCValues = { MarkupBuilder builder, def configurationvalues ->
        if (!configurationvalues) return
        builder.configurationvalues {
            configurationvalues?.each { _cvalue ->
                cvalue(name: nameOf(_cvalue), value: valueOf(_cvalue), description: descriptionOf(_cvalue), type: typeOf(_cvalue))
            }
        }
    }

    protected Closure<MarkupBuilder> processTools = { MarkupBuilder builder, def processingtools ->
        if (!processingtools) return
        builder.processingtools {
            processingtools?.each { _tool ->
                tool(name: nameOf(_tool),
                        value: valueOf(_tool),
                        basepath: readAttribute("basepath", _tool)) {
                    processResourcesets(builder, _tool["resourcesets"])
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

    protected Closure<MarkupBuilder> processResourcesets = { MarkupBuilder builder, def resourcesets ->
        if (!resourcesets) return
        builder.resourcesets {
            resourcesets?.each { _rset ->
                rset(size: sizeOf(_rset),
                        memory: memoryOf(_rset),
                        cores: coresOf(_rset),
                        nodes: nodesOf(_rset),
                        walltime: walltimeOf(_rset)
                ) {}
            }
        }
    }

    Closure<MarkupBuilder> processFilenames = { MarkupBuilder builder, def filenames ->
        if (!(filenames)) return
        builder.filenames(
                package: readAttribute("package", filenames),
                filestagesbase: readAttribute("filestagesbase", filenames)
        ) {
            filenames?.getAt("patterns")?.each { _filename ->
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

    Closure<MarkupBuilder> processSubconfigurations = { MarkupBuilder builder, def subconfigs ->
        if (!(subconfigs)) return
        builder.subconfigurations {
            subconfigs?.each { _config ->
                configuration(
                        name: nameOf(_config),
                        inheritAnalyses: readAttribute("inheritAnalyses", _config)
                ) {
                    processConfigurationContent(builder, _config)
                }
            }
        }
    }
}
