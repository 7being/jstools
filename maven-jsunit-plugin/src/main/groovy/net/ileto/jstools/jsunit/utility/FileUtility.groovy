package net.ileto.jstools.jsunit.utility

import java.io.File;

class FileUtility {
	
	static String relativePathTo(File target, File source) {
		def toPath = target.canonicalPath
		def fromPath = source.canonicalPath
		
		toPath.each {
			if (fromPath[0] == toPath[0]) {
				toPath -= it
				fromPath -= it
			} else {
				return
			}
		}
		
		fromPath.count(File.separator).times {
			toPath = '..' + File.separator + toPath
		}
		return toPath
	}
}