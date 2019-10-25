package com.devonfw.cobigen.cli.commandtests;

import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.io.FileUtils;
import org.apache.maven.model.Dependency;
import org.apache.maven.model.Model;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.codehaus.plexus.util.xml.pull.XmlPullParserException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.devonfw.cobigen.cli.CobiGenCLI;
import com.devonfw.cobigen.cli.commands.GenerateCommand;
import com.ea.agentloader.AgentLoader;

import classloader.Agent;

/**
 * 
 */
public class UpdateCommandTest {
    
    /**
     * Logger to output useful information to the user
     */
    private static Logger logger = LoggerFactory.getLogger(CobiGenCLI.class);
    
    /** Test resources root path */
    private static String testFileRootPath = "src/test/resources/testdata/";
    
    /** pom file name */
    private static String pomFileName = "pom.xml";
    
    /** temporary pom file */
    private static String tmpPomFileName = "tmpPom.xml";
    
    /** outdated pom file */
    private static String outdatedPomFileName = "outdatedPom.xml";
    
    /** root CLI path */
    private static Path rootCLIPath = null;
    
    /**
     * We need to dynamically load the Java agent before the tests.
     * Note that Java 9 requires -Djdk.attach.allowAttachSelf=true to be present among JVM startup arguments.
     */
    @Before
    public void loadJavaAgent() {
        AgentLoader.loadAgentClass(Agent.class.getName(), "");
    }
    
    /** TODO: rootCLI path setting 
     * 
     * @throws URISyntaxException
     */
    @Before
    public void setCliPath() throws URISyntaxException {
        if (rootCLIPath == null)
        {
            File locationCLI;
            locationCLI = new File(GenerateCommand.class.getProtectionDomain().getCodeSource().getLocation().toURI());
            rootCLIPath = locationCLI.getParentFile().toPath();
        }
    }
    
    /**
     * TODO: Repalce original pom.
     * @throws URISyntaxException 
     * @throws IOException 
     */
    @Before
    public void replacePom() throws IOException  {

        File originalPom = new File(Paths.get(rootCLIPath.toString(),pomFileName).toString());
        logger.info("Pom file path" + rootCLIPath.toString());        
        File tmpPom = new File(Paths.get(testFileRootPath,tmpPomFileName).toString());
        
        // Storing original pom 
        FileUtils.copyFile(originalPom, tmpPom);
        // Replacing the original pom with an outdated one
        File outdatedPom = new File(Paths.get(testFileRootPath,outdatedPomFileName).toString());
        FileUtils.copyFile(outdatedPom,originalPom);
        
    }
    
    
    /**
     * TODO: Restore original pom.
     * @throws URISyntaxException 
     * @throws IOException 
     */
    
    @After
    public void restorePom() throws IOException {
        File originalPom = new File(Paths.get(rootCLIPath.toString(),pomFileName).toString());
        File tmpPom = new File(Paths.get(testFileRootPath,tmpPomFileName).toString());
        
        // Restoring original pom 
        FileUtils.copyFile(tmpPom,originalPom);
        
    }
    
    /**
     *  Read pom file
     */
    private List<Dependency> readPom(File pomFile) throws FileNotFoundException, IOException, XmlPullParserException
    {
        if (pomFile.exists())
        {
            MavenXpp3Reader reader = new MavenXpp3Reader();
            Model model = reader.read(new FileReader(pomFile));
            List<Dependency> pomDependencies = model.getDependencies();
            return pomDependencies;
        }
        return new ArrayList<>();

    }
    
    /**
     * TODO
     * @param artifactId
     * @return
     */
    private String getArtifactVersion(File pomFile, String artifactId) {
        String version = null;
        
        List<Dependency> dependencies;
        try {
            dependencies = readPom(pomFile);
            
            // Get plugin version
            Optional<Dependency> matchingObject = dependencies.stream().
                filter(p -> p.getArtifactId().equals(artifactId)).
                findFirst();
            
            version = matchingObject.get().getVersion();
        } catch (IOException | XmlPullParserException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        return version;
    }

    /**
     * Update test
     * @throws URISyntaxException 
     * @throws IOException 
     * @throws XmlPullParserException 
     */
    @Test
    public void pluginUpdateTest() throws IOException, XmlPullParserException {
        String pluginId = "tsplugin";
        File originalPom = new File(Paths.get(rootCLIPath.toString(),pomFileName).toString());
       
        String oldVersion = getArtifactVersion(originalPom, pluginId);
        logger.info("tsplugin version before update: " + oldVersion);
        
        assertNotNull(oldVersion);
        
        String args[] = new String[1];
        args[0] = "update";
        CobiGenCLI.main(args);
        
        File updatedPom = new File(Paths.get(rootCLIPath.toString(),pomFileName).toString());       
        String newVersion = getArtifactVersion(updatedPom, pluginId);
        
        assertNotNull(newVersion);
        assertNotEquals(oldVersion, newVersion);
        
        logger.info("tsplugin version after update: " + newVersion);
        
    }

}
