package net.tiny.service;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

public class NativeLibraryLoaderService implements NativeLibraryLoader {

	private static Logger LOGGER = Logger.getLogger(NativeLibraryLoader.class.getName());

	private List<String> libraries = new ArrayList<String>();

	@Override
	public List<String> getLibraries() {
		return libraries;
	}

	@Override
	public boolean isLoaded(String lib) {
		return libraries.contains(lib);
	}

	@Override
	public boolean load(String lib) {
		if(!isLoaded(lib)) {
			// (Windows: XYZ.dll) (Linux: libXYZ.so)
			System.loadLibrary(lib);
			libraries.add(lib);
			return true;
		}
		return false;
	}

	public void setLibraries(List<String> libs) {
		Throwable error = null;
		for (String lib : libs) {
			try {
				if(load(lib)) {
					LOGGER.log(Level.WARNING, "Library '" + lib +"' has been loaded.");
				}
			} catch(Throwable ex) {
				if(null == error) {
					error = ex;
				}
				LOGGER.log(Level.SEVERE, "Load '" + lib +"' failed.", ex);
			}
		}
		if(null != error) {
			throw new RuntimeException(error.getMessage(), error);
		}
	}
}
