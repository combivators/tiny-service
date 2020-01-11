package net.tiny.service;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Patterns implements Serializable {

	private static final long serialVersionUID = 1L;

	static final String LIST_REGEX = "[ ]*,[ ]*";

	private List<Pattern> includes =
		Collections.synchronizedList(new ArrayList<Pattern>());

	private List<Pattern> excludes =
		Collections.synchronizedList(new ArrayList<Pattern>());

	public Patterns() {
	}

	public Patterns(String include) {
		setInclude(include);
	}

	public Patterns(String include, String exclude) {
		setInclude(include);
		setExclude(exclude);
	}

	public List<Pattern> getInclude() {
		return this.includes;
	}

	public void setInclude(String param) {
		setPattern(param, includes);
	}

	public List<Pattern> getExclude() {
		return this.excludes;
	}

	public void setExclude(String param) {
		setPattern(param, excludes);
	}

	private void setPattern(String param, List<Pattern> list) {
		if(null != param && param.length() > 0) {
			list.clear();
			String[] regexs = param.split(LIST_REGEX);
			for(String regex : regexs) {
				list.add(Pattern.compile(regex.trim()));
			}
		}
	}

	public boolean invaild(String data) {
		return !vaild(data);
	}

	public boolean vaild(String data) {
		if(includes.size() > 0) {
			if(include(data)) {
				if(excludes.size() > 0) {
					if(exclude(data)) {
						return false;
					} else {
						return true;
					}
				} else {
					return true;
				}
			} else {
				return false;
			}
		} else {
			if(excludes.size() > 0) {
				if(exclude(data)) {
					return false;
				} else {
					return true;
				}
			} else {
				return true;
			}
		}
	}

	private boolean include(String data) {
		return matcher(data, includes);
	}


	private boolean exclude(String data) {
		return matcher(data, excludes);
	}

	protected boolean matcher(String data, Collection<Pattern> patterns) {
		Matcher m;
		for(Pattern pattern : patterns) {
			m = pattern.matcher(data);
			if(m.matches()) {
				return true;
			}
		}
		return false;
	}

	@Override
	public String toString() {
		StringBuffer sb = new StringBuffer();
		sb.append("include [");
		int i = 0;
		for(Pattern pattern : includes) {
			if(i>0)
				sb.append(", ");
			sb.append(pattern.pattern());
			i++;
		}
		sb.append("],  exclude [");
		i = 0;
		for(Pattern pattern : excludes) {
			if(i>0)
				sb.append(", ");
			sb.append(pattern.pattern());
			i++;
		}
		sb.append("]");
		return sb.toString();
	}

	/**
	 * <p>
	 * Example:</br> Patterns<String> patterns =
	 * Patterns.valueOf("l,2,3, !a, !b, !2");</br>
	 * "1", "3" is 0K</br>
	 * "a", "b", "2" is NG</br>
	 * </p>
	 *
	 * @param value
	 * @return true: OK, false: NG
	 **/
	public static Patterns valueOf(String value) {
		List<String> include = new ArrayList<String>();
		List<String> exclude = new ArrayList<String>();
		if (!value.isEmpty()) {
			String[] codes = value.split(",[ ]*");
			for (String code : codes) {
				if (code.startsWith("!")) {
					exclude.add(code.substring(1));
				} else {
					include.add(code);
				}
			}
		}
		StringBuilder includePatterns = new StringBuilder();
		if(!include.isEmpty()) {
			for(String p : include) {
				if(includePatterns.length() > 0) {
					includePatterns.append(",");
				}
				includePatterns.append(p);
			}
		}
		StringBuilder excludePatterns = new StringBuilder();
		if(!exclude.isEmpty()) {
			for(String p : exclude) {
				if(excludePatterns.length() > 0) {
					excludePatterns.append(",");
				}
				excludePatterns.append(p);
			}
		}
		return new Patterns(includePatterns.toString(), excludePatterns.toString());
	}
}
