package de.dkfz.roddy.config

import de.dkfz.roddy.knowledge.files.GenericFileGroup
import de.dkfz.roddy.tools.BufferValue
import de.dkfz.roddy.tools.TimeUnit
import de.dkfz.roddy.tools.Tuple3
import groovy.transform.TypeCheckingMode
import groovy.util.slurpersupport.NodeChild
import org.junit.BeforeClass
import org.junit.Test;
import org.junit.rules.TemporaryFolder
import static ResourceSetSize.*;

import java.lang.reflect.Method

/**
 * Tests for ConfigurationFactory
 */
@groovy.transform.CompileStatic
public class ConfigurationFactoryTest {

    public static TemporaryFolder tempFolder = new TemporaryFolder();

    private static File testFolder1

    private static File testFolder2

    private static File testFolder3

    private static File testFolder4

    @BeforeClass
    public static void setupClass() {

        // Create buggy directories first.
        tempFolder.create();

        testFolder1 = tempFolder.newFolder("normalFolder1");
        testFolder2 = tempFolder.newFolder("normalFolder2");
        testFolder3 = tempFolder.newFolder("buggyCfgFolder1");
        testFolder4 = tempFolder.newFolder("buggyCfgFolder2");


        LinkedHashMap<String, File> configurationFiles = [
                "project_A"  : testFolder1,
                "project_B"  : testFolder1,
                "project_C"  : testFolder1,
                "project_D"  : testFolder2,
                "project_E"  : testFolder2,
                "project_F"  : testFolder2,
                "something_A": testFolder1,
                "something_B": testFolder2,

                "project_G"  : testFolder3,
                "project_H"  : testFolder3,
                "project_I"  : testFolder3,
                "project_J"  : testFolder4,
                "project_K"  : testFolder4,
                "project_L"  : testFolder4,
                "something_C": testFolder3,
                "something_D": testFolder4,
        ]

        configurationFiles.each {
            String k, File f ->
                if (k.startsWith("project")) {
                    new File(f, "${k}.xml") << "<configuration configurationType='project' name='project_${k}'></configuration>";
                } else if (k.startsWith("something")) {
                    new File(f, "${k}.xml") << "<configuration name='standard_${k}'></configuration>";
                }
        }
        testFolder3.setReadable(false);
        testFolder4.setReadable(true, true);
    }

    @Test
    public void testLoadInvalidConfigurationDirectories() {
        // Load context from invalid directories and see, if the step fails.
        ConfigurationFactory.initialize([testFolder3, testFolder4])

        testFolder3.setReadable(true);
        testFolder4.setReadable(true);

        assert ConfigurationFactory.getInstance().getAvailableProjectConfigurations().size() == 3;
        assert ConfigurationFactory.getInstance().getAvailableConfigurationsOfType(Configuration.ConfigurationType.OTHER).size() == 1;
    }

    @Test
    public void testLoadValidConfigurationDirectories() {
        // Load context from valid directories and see, if the step fails.
        ConfigurationFactory.initialize([testFolder1, testFolder2])

        assert ConfigurationFactory.getInstance().getAvailableProjectConfigurations().size() == 6;
        assert ConfigurationFactory.getInstance().getAvailableConfigurationsOfType(Configuration.ConfigurationType.OTHER).size() == 2;
    }

    @Test
    public void testParseFileGroupWithDefinedChildTypes() {
        String xml =
                """
                    <output type="filegroup" typeof="de.dkfz.roddy.knowledge.files.GenericFileGroup">
                        <output type="file" typeof="TestFile1" scriptparameter="FILENAME_1"/>
                        <output type="file" typeof="TestFile2" scriptparameter="FILENAME_2"/>
                    </output>
                """
        NodeChild nc = (NodeChild) new XmlSlurper().parseText(xml);
        ToolEntry.ToolFileGroupParameter tparm = new ConfigurationFactory([]).parseFileGroup(nc, "testTool")
        assert tparm != null;
        assert tparm.groupClass == GenericFileGroup.class;
        assert tparm.files[0].fileClass.name == "de.dkfz.roddy.synthetic.files.TestFile1"
        assert tparm.files[1].fileClass.name == "de.dkfz.roddy.synthetic.files.TestFile2"
    }

    @Test
    public void testParseFileGroupWithDefinedClass() {
        String xml = """<output type="filegroup" typeof="de.dkfz.roddy.knowledge.files.GenericFileGroup" fileclass="TestFile" />"""
        NodeChild nc = (NodeChild) new XmlSlurper().parseText(xml);
        ToolEntry.ToolFileGroupParameter tparm = new ConfigurationFactory([]).parseFileGroup(nc, "testTool")
        assert tparm.isGeneric()
        assert tparm.getGenericClassString() == "de.dkfz.roddy.knowledge.files.GenericFileGroup<de.dkfz.roddy.synthetic.files.TestFile>"
        assert tparm.passOptions == ToolEntry.ToolFileGroupParameter.PassOptions.parameters;
    }

    @Test
    public void testParseFileGroupWithUndefinedClass() {
        String xml = """<output type="filegroup" typeof="GenericFileGroup2" fileclass="TestFile" />"""
        NodeChild nc = (NodeChild) new XmlSlurper().parseText(xml);
        ToolEntry.ToolFileGroupParameter tparm = new ConfigurationFactory([]).parseFileGroup(nc, "testTool")
        assert tparm.isGeneric()
        assert tparm.getGenericClassString() == "de.dkfz.roddy.synthetic.files.GenericFileGroup2<de.dkfz.roddy.synthetic.files.TestFile>"
        assert tparm.passOptions == ToolEntry.ToolFileGroupParameter.PassOptions.parameters;
    }

    private NodeChild getValidToolResourceSetNodeChild() {
        NodeChild xml = (NodeChild) new XmlSlurper().parseText(
                """
                    <resourcesetsample>
                        <rset size="s" memory="3" cores="2" walltime="00:04" queue="ultrafast" nodeflag="testweise"/>
                        <rset size="m" memory="3g" cores="1" nodes="1" walltime="12"/>
                        <rset size="l" memory="300M" cores="2" nodes="1" walltime="120m"/>
                        <rset size="xl" memory="2T" cores="2" nodes="1" walltime="180h"/>
                    </resourcesetsample>
                """
        );
        return xml;
    }

    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    private Collection<NodeChild> getNodeChildsOfValidResourceEntries() {
        def resourceSetNodeChild = getValidToolResourceSetNodeChild()
        return resourceSetNodeChild.rset as Collection<NodeChild>;
    }

    @Test
    public void testParseToolResourceSet() {
        Method parseToolResourceSet = ConfigurationFactory.getDeclaredMethod("parseToolResourceSet", NodeChild, Configuration);
        parseToolResourceSet.setAccessible(true);

        Map<ResourceSetSize, ToolEntry.ResourceSet> rsets = [:];

        getNodeChildsOfValidResourceEntries().each { rset ->
            ToolEntry.ResourceSet result = (ToolEntry.ResourceSet) parseToolResourceSet.invoke(null, rset, null);
            assert result;
            assert result.size; //Check if the size has a valid value.

            rsets[result.size] = result;
        }

        // Check the example datasets and see, if all necessary values were set and converted properly.

        assert rsets[s].mem instanceof BufferValue && rsets[s].mem.toString() == "3072M"; // Check 3 gigabyte
        assert rsets[s].cores instanceof Integer && rsets[s].cores == 2;
        assert rsets[s].walltime instanceof TimeUnit && rsets[s].walltime.toString() == "00:00:00:04";
        assert rsets[s].queue instanceof String && rsets[s].queue == "ultrafast";
//        assert rsets[s].queue instanceof TimeUnit && rsets[s].queue == "testweise";

        assert rsets[m].mem instanceof BufferValue && rsets[m].mem.toString() == "3072M"; // Check 3 gigabyte
        assert rsets[m].cores instanceof Integer && rsets[m].cores == 1;
        assert rsets[m].nodes instanceof Integer && rsets[m].nodes == 1;
        assert rsets[m].walltime instanceof TimeUnit && rsets[m].walltime.toString() == "00:12:00:00";

        assert rsets[l].mem instanceof BufferValue && rsets[l].mem.toString() == "300M"; // 300 megabyte
        assert rsets[l].cores instanceof Integer && rsets[l].cores == 2;
        assert rsets[l].nodes instanceof Integer && rsets[l].nodes == 1;
        assert rsets[l].walltime instanceof TimeUnit && rsets[l].walltime.toString() == "00:02:00:00"; // 120m == 2h

        assert rsets[xl].mem instanceof BufferValue && rsets[xl].mem.toString() == "2097152M"; // 300 megabyte
        assert rsets[xl].cores instanceof Integer && rsets[xl].cores == 2;
        assert rsets[xl].nodes instanceof Integer && rsets[xl].nodes == 1;
        assert rsets[xl].walltime instanceof TimeUnit && rsets[xl].walltime.toString() == "07:12:00:00"; // 180h
    }

    private static final String STR_VALID_DERIVEDFROM_PATTERN = "<filename class='TestFileWithParent' derivedFrom='TestParentFile' pattern='/tmp/onderivedFile'/>"
    private static final String STR_VALID_DERIVEDFROM_PATTERN_WITH_ARR = "<filename class='TestFileWithParent' derivedFrom='TestParentFile[2]' pattern='/tmp/onderivedFile'/>"
    private static final String STR_VALID_ONMETHOD_PATTERN = "<filename class='TestFileOnMethod' onMethod='de.dkfz.roddy.knowledge.files.BaseFile.getFilename' pattern='/tmp/onMethod'/>"
    private static final String STR_VALID_ONSCRIPT_PATTERN = "<filename class='TestFileOnScript' onScript='testScript' pattern='/tmp/onScript'/>"
    private static final String STR_VALID_FILESTAGE_PATTERN = "<filename class='FileWithFileStage' fileStage=\"GENERIC\" pattern='/tmp/filestage'/>"

    private static NodeChild parseXML(String xml) {
        return (NodeChild) new XmlSlurper().parseText(xml);
    }

    private NodeChild getValidFilenamePatternsNodeChild() {
        return parseXML(
                """
                    <xml>
                        <filenames filestagesbase='de.dkfz.roddy.knowledge.files.FileStage'>
                            ${STR_VALID_DERIVEDFROM_PATTERN}
                            ${STR_VALID_DERIVEDFROM_PATTERN_WITH_ARR}
                            ${STR_VALID_ONMETHOD_PATTERN}
                            ${STR_VALID_ONSCRIPT_PATTERN}
                            ${STR_VALID_FILESTAGE_PATTERN}
                        </filenames>
                    </xml>
                """
        );
    }

    private NodeChild getValidFilenamePatternForDerivedFromPatternType() { return parseXML("<filenames filestagesbase='de.dkfz.roddy.knowledge.files.FileStage'>${STR_VALID_DERIVEDFROM_PATTERN}</filenames>"); }

    private NodeChild getValidFilenamePatternForDerivedFromPatternTypeWithArr() { return parseXML("<filenames filestagesbase='de.dkfz.roddy.knowledge.files.FileStage'>${STR_VALID_DERIVEDFROM_PATTERN_WITH_ARR}</filenames>"); }

    private NodeChild getValidFilenamePatternForOnMethodPatternType() { return parseXML("<filenames filestagesbase='de.dkfz.roddy.knowledge.files.FileStage'>${STR_VALID_ONMETHOD_PATTERN}</filenames>"); }

    private NodeChild getValidFilenamePatternForOnScriptPatternType() { return parseXML("<filenames filestagesbase='de.dkfz.roddy.knowledge.files.FileStage'>${STR_VALID_ONSCRIPT_PATTERN}</filenames>"); }

    private NodeChild getValidFilenamePatternForFileStagePatternType() { return parseXML("<filenames filestagesbase='de.dkfz.roddy.knowledge.files.FileStage'>${STR_VALID_FILESTAGE_PATTERN}</filenames>"); }

    @Test
    public void testLoadPatternClassWithNullAndSyntheticClass() {

        Tuple3<Class, Boolean, Integer> loadPatternClassResult = ConfigurationFactory.loadPatternClass((String) null, "ASyntheticTestClass", (Class) null);
        assert loadPatternClassResult != null;
        assert ((Class)loadPatternClassResult.x).getName().endsWith("ASyntheticTestClass")
    }

    private Map<String, FilenamePattern> readFilenamePatterns(NodeChild nodeChild) {
        Method m = ConfigurationFactory.class.getDeclaredMethod("readFilenamePatterns", NodeChild);
        m.setAccessible(true);
        return m.invoke(null, nodeChild) as Map<String, FilenamePattern>;
    }

    @Test
    public void testReadFilenamePatterns() {
        Map<String, FilenamePattern> filenamePatterns = readFilenamePatterns(getValidFilenamePatternsNodeChild());
        assert filenamePatterns.size() == 4;
    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testReadFilenamePatternsForDerivedFromPatternType() {
        NodeChild xml = getValidFilenamePatternForDerivedFromPatternTypeWithArr()
        DerivedFromFilenamePattern fpattern = ConfigurationFactory.readDerivedFromFilenamePattern("de.dkfz.roddy.knowledge.files.FileStage", xml.filename.getAt(0)) as DerivedFromFilenamePattern;
        assert fpattern.filenamePatternDependency == FilenamePatternDependency.derivedFrom;
        assert fpattern.derivedFromCls.name.endsWith("TestParentFile");
        assert fpattern.cls.name.endsWith("TestFileWithParent");
    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testReadFilenamePatternsForDerivedFromPatternTypeWithArr() {
        NodeChild xml = getValidFilenamePatternForDerivedFromPatternType()
        DerivedFromFilenamePattern fpattern = ConfigurationFactory.readDerivedFromFilenamePattern("de.dkfz.roddy.knowledge.files.FileStage", xml.filename.getAt(0)) as DerivedFromFilenamePattern;
        assert fpattern.filenamePatternDependency == FilenamePatternDependency.derivedFrom;
        assert fpattern.derivedFromCls.name.endsWith("TestParentFile");
        assert fpattern.cls.name.endsWith("TestFileWithParent");
    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testReadFilenamePatternsForOnMethodPatternType() {
        NodeChild xml = getValidFilenamePatternForOnMethodPatternType()
        OnMethodFilenamePattern fpattern = ConfigurationFactory.readOnMethodFilenamePattern("de.dkfz.roddy.knowledge.files", xml.filename.getAt(0)) as OnMethodFilenamePattern;
        assert fpattern.filenamePatternDependency == FilenamePatternDependency.onMethod;
        assert fpattern.cls.name.endsWith("TestFileOnMethod");
    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testReadFilenamePatternsForOnToolPatternType() {
        NodeChild xml = getValidFilenamePatternForOnScriptPatternType()
        OnToolFilenamePattern fpattern = ConfigurationFactory.readOnScriptFilenamePattern("de.dkfz.roddy.knowledge.files.FileStage", xml.filename.getAt(0)) as OnToolFilenamePattern;
        assert fpattern.filenamePatternDependency == FilenamePatternDependency.onTool;
        assert fpattern.cls.name.endsWith("TestFileOnScript");
    }

    @Test
    @groovy.transform.CompileStatic(TypeCheckingMode.SKIP)
    public void testReadFilenamePatternsForFileStageBasedPatternType() {
        NodeChild xml = getValidFilenamePatternForFileStagePatternType()
        FileStageFilenamePattern fpattern = ConfigurationFactory.readFileStageFilenamePattern(null, "de.dkfz.roddy.knowledge.files.FileStage", xml.filename.getAt(0));
        assert fpattern.filenamePatternDependency == FilenamePatternDependency.FileStage;
        assert fpattern.cls.name.endsWith("FileWithFileStage");
    }

}
