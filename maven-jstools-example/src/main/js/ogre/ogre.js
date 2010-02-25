;(function(){
//=============================================================================
// Basis
//=============================================================================
/**
 * Ogre FW
 * @module ogre
 */
function _loadOgreLangs() {
    var override = true;
    var langs = ['Function', 'String', 'Number', 'Array', 'Hash', 'Class'];//, 'Range', 'Enum'
    // Enum, Hash and Range are Class. Avoid dirty Object proto
    for (var i = 0; i < 4; i++) {
        ogre.imports('ogre.lang.' + langs[i]);

        var native = ogre.get(window, langs[i]);
        var ogrish = ogre.get(ogre.lang, langs[i]);

        ogre.extend(native.prototype, ogrish.prototype, override);
    }

	for (var i = 4; i < 6; i++) {
		ogre.imports('ogre.lang.' + langs[i]);
	}
};

/**
 *  Determine JS eval
 *  http://webreflection.blogspot.com/2007/08/global-scope-evaluation-and-dom.html
 *	http://jquery.com
 *	
 */
function _checkJSEval() {
	var id = 'script' + (new Date).getTime();
	var script = document.createElement('script');
	script.type = 'text/javascript';
	try {
		script.appendChild(document.createTextNode('window.' + id + '=1;'));
	} catch (e) {}
	var doc = document.documentElement;
	doc.insertBefore(script, doc.firstChild);
	if (window[id]) {
		ogre.site.jsEval = true;
		delete window[id];
	}
	doc.removeChild(script);
};

/**
 * According to marks script to gain the url set
 */
function _checkJSResBase() {
	var scripts = document.getElementsByTagName('script');

	for (var i = 0; i < scripts.length; i++) {
		var src = scripts[i].src;
		var mch = /ogre.js/.exec(src);
		var idx = mch ? mch.index : 0;

		if (idx) {
		//got raw path
			src = src.substring(0, idx);
			if (/ogre\/$/.test(src)) {
				src = src.substring(0, src.length-5);
			} else {
				src += '../';
			}
		//check if is absolute url
			var startsWithProtocol = new RegExp('^' + location.protocol);
		//if not, transform
			if (!startsWithProtocol.test(src)) { 
				if (/^\//.test(src)) {
					var i = location.href.indexOf(location.host) + location.host.length;	
					var base = location.href.substring(0, i);
					src = base + src;
				} else {
					var base = location.href;
					base = base.substring(0, base.lastIndexOf('/'));
					while (/^\.\./.test(src)) {
						base = base.substring(0, base.lastIndexOf('/'));
						src = src.substring(3);
					}
					src = base + '/' + src;
				}
			}
			//if (src) {
			//	_checkJSResBase = function() { return src };
			//}
			//return src;
			if (src) {
				ogre.site.resBaseUrl = src;
			} else {
				throw new Error('Bootloader: checkJSResBase failed');
			}
		}
	}
};


ogre = {
	/**
     * Accept any kind of object, simple return it if is already an array,
     * otherwise return an array contains the passed in object.
     * 
     * @param   {Mix} o - Any kind of object.
     * @return  {Array} Array contains the passed in object or itself.
     *
     * @example
     * <pre>
	 * ogre.A()				#=>	[undefined]
	 * ogre.A(1)			#=>	[1]
	 * ogre.A({})			#=> [{}]
	 * ogre.A(function(){})	#=>	[function(){}]
	 * ogre.A([a,b,c])		#=> [a,b,c]
	 * ogre.A(arguments)	#=> Array.prototype.slice.call(arguments)
     * </pre>
	 */
    A: function(o) {
        var t = ogre.type(o);
        if (!t) return [];
        if (t == 'array') return o;
        if (t == 'arguments') return Array.prototype.slice.call(o);
        return [o];
    },

	/**
	 * ogre.F(function(){alert('a')})	#=>	function(){alert('a')}
	 * ogre.F('abc')					#=> function(){return 'abc'}
	 */
    F: function(o) {
        return (ogre.type(o) == 'function') ? o : function() {return o}; 
    },

	/**
	 * Hash
     * @return ogre.lang.Hash
	 */
    H: function(o) {
		//idea
		// ogre.H(1)				#=>	{1:undefined}
		// ogre.H('foo')			#=> {"foo": undefined}
		// ogre.H(['a', 'b'])		#=> {"a": "b"}	?? like this or put them al as keys?
		// ogre.H(['a','b'], [1,2]) #=> {"a": 1, "b": 2}
		// Function ignore in key set
		if (ogre.type(o) == 'object') {
			return new ogre.lang.Hash(o);
		}
    },

	/**
	 * Range
	 */
    R: function() {

    },

	/**
	 * Site level empty function placeholder.
	 * Must not modify
	 */
	emptyFn: function() {},
 
	/**
	 * Scope safe eval 
	 */
	eval: function(str) {
		return window.eval(str);
	},

	/**
	 * Get the value of the object using the string specified as key
	 *
	 * h = {a: 1, b: 2, c:{k1: 3, k2: 4}}
	 *
	 * ogre.get(h, 'a')			#=>	1
	 * ogre.get(h, 'c.k2')		#=> 4
	 */
	get: function(o, s) {
		var t = ogre.type(o);
		if (!t) {
			return undefined;
		} else if (t == 'string') {
			return ogre.get(window, o);
		} else {
			var s = s.join ? s : s.split('.');
			var k = s.shift();
			return s.length ? (o[k] ? ogre.get(o[k], s) : undefined) : o[k];
		}
	},

	/**
	 * h = {a: 1, b: 2, c:{k1: 3, k2: 4}}
	 *
	 * ogre.has(h, 'a')			#=> true
	 * ogre.has(h, 'a.c')		#=> false
	 */
	has: function(o, s) {
		return ogre.get(o, s) !== undefined;
	},

	/**
	 * h = {}
	 *
	 * ogre.set(h, 'a.b.c', 1)	#=> h === {a: {b: {c: 1}}}
	 */
	set: function(o, s, v) {
		if (!ogre.type(o)) return undefined;
		var n = s.split('.');
		var k = n.pop();
		for (var i = 0; i < n.length; i++) {
			o = o[n[i]] = o[n[i]] || {};
		}
		return  k ? o[k] = v : undefined;
	},
	
	/**
	 * Return string to identify this type of object passed in, return
	 * false if the passed in object is null or undefined
	 *
     * @param   {Mix} o Any kind of js object.
     * @return  {String} A string presents the type of the object passed in.
     *
     * @example
     * <pre>
	 * ogre.type()				#=> false
	 * ogre.type(null)			#=> false
	 * ogre.type(undefined)		#=> false
	 * ogre.type(1)				#=> 'number'
	 * ogre.type(Number(1))		#=>	'number'
	 * ogre.type('abc')			#=> 'string'
	 * ogre.type(String('abc')) #=>	'string'
	 * ogre.type([])			#=> 'array'
	 * ogre.type(new Array)		#=> 'array'
	 * ogre.type(arguments)		#=> 'arguments' // works except opera. ogre.type(arguments) #=> 'array' in opera
	 * ogre.type(ogre.emptyFn)	#=> 'function'
	 * ogre.type({})			#=> 'object'
	 * ogre.type(new Date)		#=> 'date'
	 * ogre.type(//)			#=> 'regexp'
	 *
	 * others used on doc have 'element', 'textnode', 'whitespace', nodeList'
     * </pre>
	 */
	type: function(o) {
		if (o === undefined || o === null) return false;
		if (o.htmlElement) return 'element';
		var t = typeof o;
		if (t == 'object') {
			if (o instanceof String) return 'string';
			if (o instanceof Number) return 'number';
			if (o instanceof Boolean)return 'boolean';
			if (o instanceof Date) return 'date';
			if (o.nodeName) switch (o.nodeType) {
					case 1: return 'element';
					case 3: return (/\S/).test(o.nodeValue) ? 'textnode' : 'whitespace';
			}
		}
		if (t == 'object' || t == 'function') {
			switch(o.constructor) {
				case Array: return 'array';
				case RegExp: return 'regexp';
			}
			if (typeof o.length == 'number') {
				if (o.callee) return 'arguments';
				if (typeof o.item == 'function') return 'nodeList';
			}
		}
		return t;
	},

	/**
	 * Deep copy Array, Object, Date, link others, like function, regexp
	 * a = [1, 'a', //g, function(){}, {k: 'val'}]
	 * b = ogre.clone(a)
	 *
	 * a !== b
	 * a[0] === b[0]
	 * a[1] === b[1]
	 * a[2] === b[2]
	 * a[3] === b[3]
	 * a[4] !== b[4]
	 * a[4]['k'] === b[4]['k']
	 */
	clone: function(o, keeplink) {
		if (!o) return o;
		var t = ogre.type(o);
		if (t == 'array') {
			var c = [];
			for (var i = 0, l = o.length; i < l; i++) {
				c.push(ogre.clone(o[i], keeplink));
			}
			return c;
		} 
		if (t == 'object') {
			var c = {};
			for (var k in o) if (Object.prototype.hasOwnProperty.call(o, k)) {
				c[k] = ogre.clone(o[k], keeplink);	
			}
			return c;
		} 
		if (t == 'date') return new Date(+o);
		if (t == 'string' || t == 'number') return o;
		if (keeplink) return o;
	},

	/**
	 * a = [1, 2, 3, {k1: 'foo', k2: String('bar'}]
	 * b = [1, 2, 3, {k1: 'foo', k2: 'bar'}]
	 * ogre.equal(a, b)			#=> true
	 *
	 * h1 = {k1: 'a', k2: 'b', k3: 'c'}
	 * h2 = {k2: 'b', k1: 'a', k3: 'c'}
	 * ogre.equal(h1, h2)		#=>	true
	 *
	 */
	equal: function(o1, o2) {
		var t1 = ogre.type(o1), t2 = ogre.type(o2);
		if (t1 != t2) return false;
		if (!t1 || o1 == o2) return true;
		if (t1 == 'array') {
			if (o1.length != o2.length) return false;
			for (var i = 0, l = o1.length; i < l; i++) {
				if (!ogre.equal(o1[i], o2[i])) return false;
			}
			return true;
		}
		if (t1 == 'object') {
			var c = 0;
			for (var k in o1) c++;
			for (var k in o2) c--;
			if (c) return false;
//hasOwnProperty?
			for (var k in o1) {
				if (!ogre.equal(o1[k], o2[k])) return false;
			}
			return true;
		} 
		if (t1 == 'date') return +o1 == +o2;
		return false;
	},

	/**
	 * "Copy" all the properties in one object to another object.
	 * Important: "Copy" means copy the literal values, link the
	 * 		object values. Same properties will be overriden. 
	 * @param	{Object} o1 - The object want to copy properties from other object.
	 * @param	{Object} o2 - The object properties copy from
	 * @param	{Boolean} override - Whether override, default true
	 * @return	The extended original object.
	 */
	extend: function(o1, o2, override) {
		if (!o2) return o1;
		if (override !== false) {
			for (var k in o2) o1[k] = o2[k];
			return o1;
		}
		for (var k in o2) if (o1[k] === undefined) o1[k] = o2[k];
		return o1;
	},

	/**
	 * Sometimes, I think the merge deep is too heavy for huge data..
	 * Be careful to avoid using with huge data, like long array.
	 */
	merge: function(o1, o2, o3) {
		var mix = {}; 
		for (var i = 0, l = arguments.length; i < l; i++){
			var o = arguments[i];
			if (ogre.type(o) == 'object') for (var k in o) {
				var op = o[k], mp = mix[k];
				mix[k] = (mp && ogre.type(op) == 'object' && ogre.type(mp) == 'object') ? ogre.merge(mp, op) : ogre.clone(op);
			}
		}
		return mix;       
	}
};

//=============================================================================
// Advanced
//=============================================================================//=
ogre.extend(ogre, {
	/**
	 * name it site or config? I want to addin browser detection info here...
	 */
	site: {
		//----------------------------------------------------------------------------
		// comment me...
		// run once is enough. Extract method, needed? Because only run once...
		// Or better move into the closure. Now kept..
		// Kept or run once and free the memory?
		//----------------------------------------------------------------------------
		jsEval: false,
        resBaseUrl: ''
	},

	/**
	 * document me...
     */
	imports: function(varargs) {
		for (var i = 0; i < arguments.length; i++) {
			var classname = arguments[i];
			if (classname) {
				var path = classname.replace(/(\.|\\\.)/g, function(match) {
					return match == '.' ? '/' : '.';		
				});
				ogre.include(path + '.js');
			}
		}
	},

	/**
	 * document me...
	 */	
	include: function(file) {
		if (!file) {
			return;
		}
		var url = ogre.site.resBaseUrl + file;

		if (__scripts[url]) {
			return;
		}

		var js = __download(url);
		js = '/*' + url + ' */\n' + js; // same as oltk, easier to spot in firebug

		__scripts[url] = true;
		ogre.evalScript(js);
		__scripts.push(url); // ?
	},

	/**
	 * document me...
	 */
	namespace: function(nspace) {
		var obj = ogre.get(window, nspace);
		if (ogre.type(obj) != 'object') {
			obj = ogre.set(window, nspace, {});
		} //better throw warn if ns is already
		  //defined and not object
		return obj;
	},

	evalScript: function(js) {
		
	    /*
	    return ogre.eval(js);
        if (js) {
            var head = document.getElementsByTagName('head')[0] || document.documentElement,
				script = document.createElement('script');
            script.type = 'text/javascript';
			if (ogre.site.jsEval) {
                script.appendChild(document.createTextNode(js));    
            } else {
                script.text = js;
            }

            head.insertBefore(script, head.firstChild);
            head.removeChild(script);
        }
	    */
		
        // IE
        if (window.execScript) {
            window.execScript(js);
        }
        // SAFARI
        else if (/KHTML/.test(navigator.userAgent)
                || (/AppleWebKit\/([^\s]*)/).test(navigator.userAgent)) {
            // see
            // http://webreflection.blogspot.com/2007/08/global-scope-evaluation-and-dom.html
            var script = document.createElement('script');
            script.type = 'text/javascript';
            script.appendChild(document.createTextNode(js));
            var head = document.getElementsByTagName('head')[0]
                    || document.documentElement;
            head.insertBefore(script, head.firstChild);
            head.removeChild(script);
        }
        // MOZILLA, OPERA
        else {
            window.eval(js);
        }
		

    },

	/**
	 * Access to jQuery's ajax object
	 */
	ajax: function() {
		if (ogre.get(jQuery, 'ajax')) {
			ogre.ajax = jQuery.ajax;
		}  
	}
});

//=============================================================================
// PRIVATE IMPLEMENTATION
//=============================================================================

//-----------------------------------------------------------------------------
// comment me...
//-----------------------------------------------------------------------------
function __bootloader() {
	_checkJSEval();
    _checkJSResBase();
    _loadOgreLangs();
};


//-----------------------------------------------------------------------------
// comment me...
//-----------------------------------------------------------------------------
/**
 * document me...
 */
var __scripts = [];

//-----------------------------------------------------------------------------
// comment me...
// It's not designed for generate use purpose, there are many better choice
// to use XHR, like jQuery, Sarissa, we definitely will integrate jQuery.
//-----------------------------------------------------------------------------
/**
 * document me...
 * Supported ActiveX controls to determine whether
 * Create a corresponding object
 * Initialize the object and send the request
 * Determine the request status and the way back to the text string message
 * Or thrown out of abnormal
 */
function __download(url) {
	var xhr = window.ActiveXObject ? new ActiveXObject('MSXML2.XMLHTTP')
								   : new XMLHttpRequest();
	xhr.open('GET', url, false);
	xhr.send(null);
	if (__isDocumentOk(xhr)) {
		return xhr.responseText;
	}
	throw 'ogre.__download: url ' + url + ' status: ' + xhr.status;
};

//-----------------------------------------------------------------------------
// comment me...
//-----------------------------------------------------------------------------
/**
 * document me...
 * Copy from oltk, because it's very nice.
 * To determine the current state of HTTP
 */
function __isDocumentOk(http) {
	var stat = http.status || 0;
	return ( (stat >= 200) && (stat < 300) ) || // allow any 2XX response code
		(stat == 304)  || // get it out of the cache
		(stat == 1223) || // IE mangled the status code
		(!stat && (location.protocol == 'file:' || location.protocol == 'chrome:'));
};


__bootloader();

})();

//	EOF
