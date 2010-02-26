package net.ileto.jstools.utility

import org.apache.commons.lang.StringUtils;

class FileUtility {
	
	def static relativePathTo(File target, File source) {
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
		toPath
	}
	
	
	def static replaceStart(File f, File start, File replace) {
		String path = StringUtils.removeStart(f.path, start.path)
		path = StringUtils.removeStart(path, File.pathSeparator)
		new File(replace, path)
	}
	
	def static replaceExtension(File f, String ext) {
		new File((f.path =~ /\.[^\.]+$/).replaceFirst(ext.startsWith('.') ? ext : ('.' + ext)))
	}
	
	def static trimTokens(String s, String delim) {
		List<String> result = []
		s.tokenize(delim).each {
			it = it.trim()
			if(it) {
				result << it
			}
		}
		result.join(delim)
	}
}