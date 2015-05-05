package de.dkfz.roddy.config.validation;

import de.dkfz.roddy.Roddy;
import de.dkfz.roddy.config.Configuration;
import de.dkfz.roddy.config.InformationalConfigurationContent;
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import org.xml.sax.ErrorHandler

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory;
import java.io.File
import java.util.logging.Logger;

/**
 * Created by michael on 30.04.15.
 */
public class XSDValidator {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(XSDValidator.class.getName());

    private static List<File> alreadyChecked = [];

    public static boolean validate(InformationalConfigurationContent icc) {
        if(alreadyChecked.contains(icc.file))
            return true;
        alreadyChecked << icc.file;
        String xsdString
        String xmlString
        xmlString = icc.text;
        if (icc.type == Configuration.ConfigurationType.PROJECT) {
            xsdString = RoddyIOHelperMethods.assembleLocalPath(Roddy.getRoddyBinaryFolder(), "xmlvalidation", "projectConfigurationValidation.xst").text;
        } else if(icc.type == Configuration.ConfigurationType.ANALYSIS) {
            xsdString = RoddyIOHelperMethods.assembleLocalPath(Roddy.getRoddyBinaryFolder(), "xmlvalidation", "analysisConfigurationValidation.xst").text;
        } else {
            return true;
        }

        def list = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
                .newSchema(new StreamSource(new StringReader(xsdString)))
                .newValidator().with { validator ->
            List exceptions = []
            Closure<Void> handler = { exception -> exceptions << exception }
            errorHandler = [warning: handler, fatalError: handler, error: handler] as ErrorHandler
            validate(new StreamSource(new StringReader(xmlString)))
            exceptions
        }

        if (!list)
            return true;
        logger.postAlwaysInfo("Validated file ${icc.file?.absolutePath} with errors:" )
        list.each {
            logger.postAlwaysInfo(it.toString())
        }
        return false;

    }
}
