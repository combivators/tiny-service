package net.tiny.service;

import static org.junit.jupiter.api.Assertions.*;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;

import javax.annotation.Resource;

import org.junit.jupiter.api.Test;

public class ResourceAnnotationTest {

    @Test
    public void testFindAllAnnotated() throws Exception {
        Patterns patterns = Patterns.valueOf("net.tiny.service.*, !net.tiny.service.ServiceContext");

        ClassFinder.setLoggingLevel(Level.INFO);
        ClassFinder classFinder = new ClassFinder(ClassFinder.createClassFilter(patterns));

        List<Class<?>> classes = classFinder.findClassesWithAnnotatedField(Resource.class);
        assertFalse(classes.isEmpty());
        System.out.println("ClassFinder.findClassesWithAnnotatedField(): " + classes.size());
        for (Class<?> c : classes) {
            for (Field field : c.getDeclaredFields()) {
                if (field.isAnnotationPresent(Resource.class)) {
                	Resource res = field.getDeclaredAnnotation(Resource.class);
                	System.out.println(String.format("%s.%s : @Resource(name='%s')",
                			field.getDeclaringClass().getSimpleName(), field.getName(), res.name()));
                }
            }
        }
    }

    @Test
    public void testSupplierClasses() throws Exception {
    	assertTrue(Supplier.class.isAssignableFrom(SupplierOne.class));
    	assertTrue(Supplier.class.isAssignableFrom(SupplierTwo.class));
    	assertFalse(Supplier.class.isAssignableFrom(Two.class));
    	assertFalse(SupplierTwo.class.isAssignableFrom(Supplier.class));

    	Type[] types = SupplierOne.class.getGenericInterfaces();
    	assertEquals(1, types.length);
    	assertFalse(types[0] instanceof WildcardType);
    	assertTrue(types[0] instanceof ParameterizedType);
    	ParameterizedType ptype = (ParameterizedType)types[0];
    	System.out.println(ptype.getTypeName()); //Supplier<One>

    	assertNull(ptype.getOwnerType());
    	assertNotNull(ptype.getRawType());

    	Type[] atypes = ptype.getActualTypeArguments();
    	assertEquals(1, atypes.length);
    	System.out.println(atypes[0].getTypeName());
    	assertTrue(atypes[0].equals(One.class));

        Patterns patterns = Patterns.valueOf("net.tiny.service.*, !net.tiny.service.ServiceContext");

        ClassFinder.setLoggingLevel(Level.INFO);
        ClassFinder classFinder = new ClassFinder(ClassFinder.createClassFilter(patterns));
        Class<? super Supplier<One>> supplierOne = classFinder.findSupplier(One.class);
        assertNotNull(supplierOne);
        Supplier<One> oneSupplier = (Supplier<One>)supplierOne.newInstance();
        assertSame(oneSupplier.get(), oneSupplier.get());

        Class<? super Supplier<Two>> supplierTwo = classFinder.findSupplier(Two.class);
        assertNotNull(supplierTwo);
        Supplier<Two> twoSupplier = (Supplier<Two>)supplierTwo.newInstance();
        assertNotSame(twoSupplier.get(), twoSupplier.get());

        Class<? super Supplier<Three>> supplierThree = classFinder.findSupplier(Three.class);
        assertNull(supplierThree);
    }

    public static class OneResource {
    	@Resource
    	private One one;
    }

    public static class TwoResource {
    	@Resource(name="two")
    	private Two two;
    }

    public static class ThreeResource {
    	@Resource
    	private Three three;
    }

    public static class One {

    }

    public static class Two {

    }

    public static class Three {

    }

    public static class SupplierOne implements Supplier<One> {
    	private One one = new One(); //singleton
		@Override
		public One get() {
			return one;
		}
    }

    public static class SupplierTwo implements Supplier<Two> {
		@Override
		public Two get() {
			return new Two(); //not singleton
		}
    }
}
