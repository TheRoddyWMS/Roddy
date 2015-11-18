package de.dkfz.roddy.knowledge.brawlworkflows;

import de.dkfz.roddy.config.ContextConfiguration;
import de.dkfz.roddy.core.ExecutionContext;
import org.junit.Test;

import java.lang.reflect.Method;
import java.util.LinkedHashMap;

import static org.junit.Assert.*;

/**
 * Created by heinold on 18.11.15.
 */
public class BrawlWorkflowTest {

    @Test
    public void testPrepareAndReformatLine() {

    }

    @Test
    public void testAssembleCall() {
        Method assembleCall = BrawlWorkflow.class.getDeclaredMethod("_assembleCall", String[], int, StringBuilder, ContextConfiguration, ExecutionContext, LinkedHashMap);
        assembleCall.setAccessible(true);

    }

    @Test
    public void testAssembleLoadFilesCall() {
        Method assembleLoadFilesCall = BrawlWorkflow.class.getDeclaredMethod("_assembleLoadFilesCall", int, StringBuilder, ContextConfiguration, ExecutionContext, LinkedHashMap);
        assembleLoadFilesCall.setAccessible(true);


    }

}