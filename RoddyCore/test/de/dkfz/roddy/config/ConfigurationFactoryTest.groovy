package de.dkfz.roddy.config

import de.dkfz.roddy.knowledge.files.GenericFileGroup
import groovy.util.slurpersupport.NodeChild
import org.junit.BeforeClass
import org.junit.Test;
import org.junit.rules.TemporaryFolder
import static de.dkfz.roddy.knowledge.files.GenericFileGroup.*
import java.lang.reflect.Method

/**
 * Tests for ConfigurationFactory
 */
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


        Map<String, String> configurationFiles = [
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
                        <rset size="s" memory="3" cores="2" nodes="1" walltime="00:04"/>
                        <rset size="m" memory="3g" cores="1" nodes="1" walltime="12:00"/>
                        <rset size="l" memory="300M" cores="2" nodes="1" walltime="120m"/>
                        <rset size="xl" memory="4G" cores="2" nodes="1" walltime="180h"/>
                    </resourcesetsample>
                """
        );
        return xml;
    }

    private NodeChild getInvalidToolResourceSetNodeChild() {
        NodeChild xml = (NodeChild) new XmlSlurper().parseText(
                """
                    <resourcesetsample>
                        <rset size="a" memory="3" cores="2" nodes="1" walltime="00:04"/>
                        <rset size="m" memory="3g" cores="1" nodes="1" walltime="12:00"/>
                        <rset size="l" memory="300M" cores="2" nodes="1" walltime="120m"/>
                        <rset size="xl" memory="4G" cores="2" nodes="1" walltime="180h"/>
                    </resourcesetsample>
                """
        );
        return xml;
    }

    @Test
    public void testParseToolResourceSet() {
        Method parseToolResourceSet = ConfigurationFactory.getDeclaredMethod("parseToolResourceSet", NodeChild, Configuration);
        parseToolResourceSet.setAccessible(true);

        def resourceSetNodeChild = getValidToolResourceSetNodeChild()
        resourceSetNodeChild.rset.each { rset ->
            def result = parseToolResourceSet.invoke(null, rset, null);
            assert result;
        }
    }
}

