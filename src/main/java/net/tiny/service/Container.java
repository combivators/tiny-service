package net.tiny.service;

import java.beans.BeanInfo;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.Serializable;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.IntConsumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

//Container of IoC (Inversion Of Control)
public class Container implements Serializable {

	private static final long serialVersionUID = 1L;

	private Map<MultiKey, Object> container = Collections.synchronizedMap(new HashMap<MultiKey, Object>());
	private Map<MultiKey, Class<?>> classContainer = Collections.synchronizedMap(new HashMap<MultiKey, Class<?>>());

	public int size() {
		return container.size();
	}

	public int size(boolean singleton) {
		return singleton ? (container.size() - classContainer.size()) : classContainer.size();
	}

	public boolean contains(Object name) {
		return getMultiKey(container, name) != null;
	}

	public String[] getAllKeyNames() {
		List<String> names = new ArrayList<>();
		Set<MultiKey> keys = container.keySet();
		for (MultiKey key : keys) {
			key.getKeys()
				.filter(k->(k instanceof String))
				.forEach(k -> names.add((String)k));
		}
		return names.toArray(new String[names.size()]);
	}

	public Class<?>[] getAllKeyTypes() {
		List<Class<?>> types = new ArrayList<>();
		Set<MultiKey> keys = container.keySet();
		for (MultiKey key : keys) {
			key.getKeys()
				.filter(k->(k instanceof Class))
				.forEach(k -> types.add((Class<?>)k));
		}
		return types.toArray(new Class[types.size()]);
	}

	public Class<?>[] getAllIntefaceKeys() {
		List<Class<?>> types = new ArrayList<>();
		Set<MultiKey> keys = container.keySet();
		for (MultiKey key : keys) {
			key.getKeys()
				.filter(k->((k instanceof Class) && (((Class<?>)k).isInterface())))
				.forEach(k -> types.add((Class<?>)k));
		}
		return types.toArray(new Class[types.size()]);
	}

	public boolean isSingleton(Object name) {
		MultiKey key = getMultiKey(classContainer, name);
		if (null != key)
			return false;
		key = getMultiKey(container, name);
		return (key != null) ? true : false;
	}

	private String getSimpleClassName(Object target) {
		String name = target.getClass().getName();
		//Prohibit Java standard class names
		if (name.startsWith("java.") || name.startsWith("javax.") ) {
			return null;
		}
		return  target.getClass().getSimpleName();
	}

	public void setBean(Object name, Object target) {
		setBean(name, target, true);
	}

	public void setBean(Object name, Object target, boolean singleton) {
		MultiKey key = new MultiKey(name);
		if (!container.containsValue(target) && !(name instanceof MultiKey)) {
			List<Object> keys = new ArrayList<>();
			final String scn = getSimpleClassName(target);
			keys.add(target.getClass());
			if (scn != null) {
				keys.add(scn);
			}
			List<Class<?>> faces = ClassHelper.getInterfaces(target.getClass());
			for (Class<?> i : faces) {
				keys.add(i);
			}
			if (!keys.contains(name)) {
				keys.add(name);
			}
			key = new MultiKey(keys);
		}
		setMultiKeyBean(key, target, singleton);
	}

	protected void setMultiKeyBean(MultiKey name, Object target, boolean singleton) {
		if (container.containsValue(target)) {
			if (!container.containsKey(name)) {
				// Is not same key to merge
				MultiKey org = keys(container, target)
					.findFirst()
					.get();
				container.remove(org);
				MultiKey key = org.merge(name);
				putBean(key, target);
				if (!singleton) {
					classContainer.put(key, target.getClass());
				}
			}
		} else {
			putBean(name, target);
			if (!singleton) {
				classContainer.put(name, target.getClass());
			}
		}
	}

	private void putBean(MultiKey key, Object target) {
		container.put(key, target);
	}

	protected boolean hasBean(MultiKey name) {
		return container.containsKey(name);
	}

	protected MultiKey getMultiKey(Object name) {
		MultiKey key = getMultiKey(classContainer, name);
		if (key == null) {
			key = getMultiKey(container, name);
		}
		return key;
	}

	protected MultiKey getMultiKey(Map<MultiKey, ?> map, Object key) {
		return map.keySet()
				.stream()
				.filter(k -> k.contains(key))
				.findFirst()
				.orElse(null);
	}

	protected Stream<MultiKey> keys(Map<MultiKey, ?> map, Object value) {
	    return map
	      .entrySet()
	      .stream()
	      .filter(entry -> value.equals(entry.getValue()))
	      .map(Map.Entry::getKey);
	}

	public Object getBean(Object name) {
		MultiKey key = getMultiKey(classContainer, name);
		if (key != null) {
			Class<?> classType = classContainer.get(key);
			return cloneBean(container.get(key), classType);
		} else {
			// singleton
			key = getMultiKey(container, name);
			if (null != key)
				return container.get(key);
		}
		return null;
	}

	protected List<MultiKey> getMultiKeys(Map<MultiKey, ?> map, Class<?> type) {
		return map.keySet()
				.stream()
				.filter(k -> k.contains(type))
				.collect(Collectors.toList());
	}

	public <T> T getBean(String name, Class<T> type) {
		List<MultiKey> keys = getMultiKeys(container, type);
		for (MultiKey key : keys) {
			if (key.contains(name)) {
				return type.cast(container.get(key));
			}
		}
		return null;
	}

	public <T> List<T> getBeans(Class<T> type) {
		List<MultiKey> keys = getMultiKeys(container, type);
		List<T> list = new ArrayList<>();
		for (MultiKey key : keys) {
			list.add(type.cast(container.get(key)));
		}
		return list;
	}

	public void removeBean(Object name) {
		MultiKey key = getMultiKey(classContainer, name);
		if (key != null) {
			classContainer.remove(key);
		}
		key = (key == null) ? getMultiKey(container, name) : key;
		if (null != key) {
			container.remove(key);
		}
	}

	public void destroy() {
		if(container != null) {
			container.clear();
			container = null;
		}
		if(classContainer != null) {
			classContainer.clear();
			classContainer = null;
		}
		System.gc();
	}

	@Override
	protected void finalize() throws Throwable {
		destroy();
	}

	protected <T> T cloneBean(Object target, Class<T> classType) {
		try {
			T bean = classType.newInstance();
			BeanInfo javaBean = Introspector.getBeanInfo(classType);
			PropertyDescriptor[] descriptors = javaBean.getPropertyDescriptors();
			for (PropertyDescriptor property : descriptors) {
				Method setter = property.getWriteMethod();
				Method getter = property.getReadMethod();
				if (null == setter || null == getter) {
					continue;
				}

				Object[] parameters = new Object[] { getter.invoke(target, new Object[] {}) };
				setter.invoke(bean, parameters);
			}
			return bean;
		} catch (RuntimeException e) {
			throw e;
		} catch (Exception e) {
			throw new RuntimeException(e.getMessage(), e);
		}
	}

	@Override
	public String toString() {
		return String.format("Container(singleton:%d, classes:%d)",
				(container.size() - classContainer.size()), classContainer.size());
	}

	private static Object[] filter(Collection<Object> keys, IntConsumer consumer) {
	      if (keys == null || keys.isEmpty()) {
	            throw new IllegalArgumentException("The array of keys must not be null");
	      }
	      //Remove duplicate or null data
	      final List<Object> list = keys.stream()
	    		  .filter(e -> e!=null)
	    		  .distinct()
	    		  .collect(Collectors.toList());
	      final Object[] array = list.toArray(new Object[list.size()]);
	      consumer.accept(calculateHashCode(array));
	      return array;
	}

	/**
	 * Calculate the hash code of the instance using the provided keys.
	 *
	 * @param keys the keys to calculate the hash code for
	 */
	private static int calculateHashCode(final Object... keys) {
		int total = 0;
		for (final Object key : keys) {
			total ^= key.hashCode();
		}
		return total;
	}

	/**
	 *
	 * https://stackoverflow.com/questions/6768963/multiple-keys-to-single-value-map-java
	 */
	static class MultiKey implements Serializable, Comparable<MultiKey> {
		private static final long serialVersionUID = 1L;
		private Object[] keys;
		private transient int hashCode;

		public MultiKey(Object... keys) {
			this(false, keys);
		}

		@SuppressWarnings("unchecked")
		public MultiKey(Object key) {
	      if (key == null) {
	            throw new IllegalArgumentException("The key must not be null");
	      }
	      if (key instanceof Collection) {
	    	  this.keys = filter((Collection<Object>)key, new IntConsumer() {
	    			@Override
	    			public void accept(int value) {
	    				hashCode = value;
	    			}
	    	      });
	      } else if (key instanceof MultiKey) {
	    	  this.keys = ((MultiKey)key).keys.clone();
	    	  this.hashCode = calculateHashCode(this.keys);
	      } else {
	    	  this.keys = new Object[] {key};
	    	  this.hashCode = calculateHashCode(key);
	      }
		}

		public MultiKey(final boolean cloned, Object... keys) {
	      if (keys == null || keys.length == 0) {
	            throw new IllegalArgumentException("The array of keys must not be null");
	      }
	      //Remove duplicate or null data
	      List<Object> list = Arrays.asList(keys)
	    		  .stream()
	    		  .filter(e -> e!=null)
	    		  .distinct()
	    		  .collect(Collectors.toList());
	      Object[] array = list.toArray(new Object[list.size()]);
	      if (cloned) {
	    	  this.keys = array.clone();
	      } else {
	    	  this.keys = array;
	      }
	      this.hashCode = calculateHashCode(this.keys);
		}

	    public Stream<Object> getKeys() {
	        return Arrays.asList(keys).stream();
	    }

	    public Object getKey(final int index) {
	        return keys[index];
	    }
		public int size() {
	        return keys.length;
	    }

		public boolean contains(Object key) {
			if (key == null) return false;
			if (key instanceof MultiKey) {
				final MultiKey otherMulti = (MultiKey)key;
				//Check no common element in the two specified collections
				return !Collections.disjoint(Arrays.asList(keys), Arrays.asList(otherMulti.keys));
			}
			return getKeys()
				.filter(e -> e.equals(key))
				.findFirst()
				.isPresent();
		}

		public MultiKey merge(Object key) {
			MultiKey target = new MultiKey(key);
			final List<Object> list = new ArrayList<>();
			for (Object k : this.keys) {
				list.add(k);
			}
			for (Object k : target.keys) {
				list.add(k);
			}
	  	  	this.keys = filter(list, new IntConsumer() {
	  			@Override
	  			public void accept(int value) {
	  				hashCode = value;
	  			}
	  	      });
			return this;
		}

		@Override
		public int compareTo(MultiKey o) {
			if (equals(o) )
				return 0;
			if (hashCode > o.hashCode) {
				return 1;
			} else {
				return -1;
			}
		}

	    @Override
	    public boolean equals(final Object other) {
	        if (other == this) {
	            return true;
	        }
	        if (other instanceof MultiKey) {
	            final MultiKey otherMulti = (MultiKey) other;
	            return hashCode() == otherMulti.hashCode();
	        }
	        return false;
	    }

		@Override
		public int hashCode() {
			return hashCode;
		}
	    @Override
	    public String toString() {
	        return "MultiKey" + Arrays.toString(keys);
	    }

	    /**
	     * Recalculate the hash code after deserialization. The hash code of some
	     * keys might have change (hash codes based on the system hash code are
	     * only stable for the same process).
	     * @return the instance with recalculated hash code
	     */
	    protected Object readResolve() {
	        calculateHashCode(keys);
	        return this;
	    }
	}
}