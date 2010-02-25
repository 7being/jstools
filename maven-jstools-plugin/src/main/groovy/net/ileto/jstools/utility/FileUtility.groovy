package net.ileto.jstools.utility

import java.io.File;
import org.apache.commons.lang.StringUtils

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
	
	
	static File replaceStart(File f, File start, File replace) {
		String path = StringUtils.removeStart(f.path, start.path)
		path = StringUtils.removeStart(path, File.pathSeparator)
		return new File(replace, path)
	}
	
	static File replaceExtension(File f, String ext) {
		return new File((f.path =~ /\..+$/).replaceFirst(ext.startsWith('.') ? ext : ('.' + ext)))
	}
}