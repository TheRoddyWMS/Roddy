/*
 * Copyright (c) 2016 German Cancer Research Center (Deutsches Krebsforschungszentrum, DKFZ).
 *
 * Distributed under the MIT License (license terms are at https://www.github.com/TheRoddyWMS/Roddy/LICENSE.txt).
 */

package de.dkfz.roddy.config.validation

import com.stackoverflow.questions.xmlvalidation.ResourceResolver
import de.dkfz.roddy.Roddy
import de.dkfz.roddy.StringConstants;
import de.dkfz.roddy.config.Configuration
import de.dkfz.roddy.config.loader.ConfigurationFactory;
import de.dkfz.roddy.config.PreloadedConfiguration
import de.dkfz.roddy.tools.RoddyIOHelperMethods
import org.xml.sax.ErrorHandler

import javax.xml.XMLConstants
import javax.xml.transform.stream.StreamSource
import javax.xml.validation.SchemaFactory

/**
 * Created by michael on 30.04.15.
 */
public class XSDValidator {
    private static final de.dkfz.roddy.tools.LoggerWrapper logger = de.dkfz.roddy.tools.LoggerWrapper.getLogger(XSDValidator.class.getSimpleName());

    private static List<File> alreadyChecked = [];

    public static boolean validateTree(PreloadedConfiguration icc) {
        boolean validated = validate(icc);

        String imports = icc.imports     //Get the imports of the topmost parent config
        for(PreloadedConfiguration iccP = icc; iccP; iccP = iccP.getParent()) {
            imports = iccP.imports;
        }

        if(imports) {
            for (it in imports.split(StringConstants.SPLIT_COMMA)) {
                PreloadedConfiguration iccSub = ConfigurationFactory.getInstance().getAllAvailableConfigurations()[it]
                if(iccSub)
                    validated &= validate(iccSub);
                else
                    logger.postSometimesInfo("Skipped configuration with id ${it}, not available.")
            }
        }
        return validated;
    }

    public static boolean validate(PreloadedConfiguration icc) {
        if(alreadyChecked.contains(icc.file))
            return true;
        logger.postSometimesInfo("Will validate configuration ${icc.id}.")
        alreadyChecked << icc.file;
        String xsdString
        String xmlString
        xmlString = icc.text;
        if (icc.type == Configuration.ConfigurationType.PROJECT) {
            xsdString = RoddyIOHelperMethods.assembleLocalPath(Roddy.getRoddyBinaryFolder(), "xmlvalidation", "projectConfigurationValidation.xst").text;
        } else if(icc.type == Configuration.ConfigurationType.ANALYSIS) {
            xsdString = RoddyIOHelperMethods.assembleLocalPath(Roddy.getRoddyBinaryFolder(), "xmlvalidation", "analysisConfigurationValidation.xst").text;
        } else {
            xsdString = RoddyIOHelperMethods.assembleLocalPath(Roddy.getRoddyBinaryFolder(), "xmlvalidation", "commonConfigurationValidation.xst").text;
        }
        String xsdCommonString = RoddyIOHelperMethods.assembleLocalPath(Roddy.getRoddyBinaryFolder(), "xmlvalidation", "commonValidationDefinitions.xst").text;

        def factory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI);
        factory.setResourceResolver(new ResourceResolver(RoddyIOHelperMethods.assembleLocalPath(Roddy.getRoddyBinaryFolder(), "xmlvalidation/").absolutePath));

        def list = factory.newSchema(  new StreamSource(new StringReader(xsdString))  )
                .newValidator().with { validator ->
            List exceptions = []
            Closure<Void> handler = { newException -> exceptions << newException }
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
