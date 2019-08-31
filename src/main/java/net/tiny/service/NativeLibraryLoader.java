package net.tiny.service;

import java.util.List;

public interface NativeLibraryLoader {
	boolean isLoaded(String lib);
	boolean load(String lib);
	List<String> getLibraries();
}
