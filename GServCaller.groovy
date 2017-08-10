import de.dkfz.roddy.plugins.LibrariesFactory

// Preset the libraries factory classloader! It's necessary for groovyserv to work properly. Groovyserv uses its own classloader which is not the same as the system classloader
LibrariesFactory.initializeFactory(true)
GroovyClassLoader gcl = (GroovyClassLoader) getClass().getClassLoader()
LibrariesFactory.centralGroovyClassLoader = new GroovyClassLoader(gcl)
LibrariesFactory.urlClassLoader = LibrariesFactory.centralGroovyClassLoader

try {
    de.dkfz.roddy.Roddy.main(args)
} catch (groovyx.groovyserv.SystemExitException ex) {

}
