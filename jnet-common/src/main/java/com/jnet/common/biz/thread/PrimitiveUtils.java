package com.jnet.common.biz.thread;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
/**
 * @author mugw
 * @version 1.0
 * @description
 * @date 2024/2/20 13:22:09
 */
public class PrimitiveUtils {

    private static final ConcurrentMap<String, Class<?>>  NAME_CLASS_CACHE = new ConcurrentHashMap<String, Class<?>>();
    /**
     * void(V).
     */
    public static final char JVM_VOID = 'V';

    /**
     * boolean(Z).
     */
    public static final char JVM_BOOLEAN = 'Z';

    /**
     * byte(B).
     */
    public static final char JVM_BYTE = 'B';

    /**
     * char(C).
     */
    public static final char JVM_CHAR = 'C';

    /**
     * double(D).
     */
    public static final char JVM_DOUBLE = 'D';

    /**
     * float(F).
     */
    public static final char JVM_FLOAT = 'F';

    /**
     * int(I).
     */
    public static final char JVM_INT = 'I';

    /**
     * long(J).
     */
    public static final char JVM_LONG = 'J';

    /**
     * short(S).
     */
    public static final char JVM_SHORT = 'S';

    public static boolean isPrimitives(Class<?> cls) {
        if (cls.isArray()) {
            return isPrimitive(cls.getComponentType());
        }
        return isPrimitive(cls);
    }

    public static boolean isPrimitive(Class<?> cls) {
        return cls.isPrimitive() || cls == String.class || cls == Boolean.class || cls == Character.class
                || Number.class.isAssignableFrom(cls) || Date.class.isAssignableFrom(cls);
    }

    public static boolean isPojo(Class<?> cls) {
        return ! isPrimitives(cls)
                && ! Collection.class.isAssignableFrom(cls)
                && ! Map.class.isAssignableFrom(cls);
    }

    /**
     * name to class.
     * "boolean" => boolean.class
     * "java.util.Map[][]" => java.util.Map[][].class
     *
     * @param name name.
     * @return Class instance.
     */
    public static Class<?> name2class(String name) throws ClassNotFoundException
    {
        return name2class(getClassLoader(), name);
    }

    /**
     * name to class.
     * "boolean" => boolean.class
     * "java.util.Map[][]" => java.util.Map[][].class
     *
     * @param cl ClassLoader instance.
     * @param name name.
     * @return Class instance.
     */
    private static Class<?> name2class(ClassLoader cl, String name) throws ClassNotFoundException
    {
        int c = 0, index = name.indexOf('[');
        if( index > 0 )
        {
            c = ( name.length() - index ) / 2;
            name = name.substring(0, index);
        }
        if( c > 0 )
        {
            StringBuilder sb = new StringBuilder();
            while( c-- > 0 )
                sb.append("[");

            if( "void".equals(name) ) sb.append(JVM_VOID);
            else if( "boolean".equals(name) ) sb.append(JVM_BOOLEAN);
            else if( "byte".equals(name) ) sb.append(JVM_BYTE);
            else if( "char".equals(name) ) sb.append(JVM_CHAR);
            else if( "double".equals(name) ) sb.append(JVM_DOUBLE);
            else if( "float".equals(name) ) sb.append(JVM_FLOAT);
            else if( "int".equals(name) ) sb.append(JVM_INT);
            else if( "long".equals(name) ) sb.append(JVM_LONG);
            else if( "short".equals(name) ) sb.append(JVM_SHORT);
            else sb.append('L').append(name).append(';'); // "java.lang.Object" ==> "Ljava.lang.Object;"
            name = sb.toString();
        } else {
            if( "void".equals(name) ) return void.class;
            else if( "boolean".equals(name) ) return boolean.class;
            else if( "byte".equals(name) ) return byte.class;
            else if( "char".equals(name) ) return char.class;
            else if( "double".equals(name) ) return double.class;
            else if( "float".equals(name) ) return float.class;
            else if( "int".equals(name) ) return int.class;
            else if( "long".equals(name) ) return long.class;
            else if( "short".equals(name) ) return short.class;
        }

        if( cl == null ){
            cl = getClassLoader();
        }
        Class<?> clazz = NAME_CLASS_CACHE.get(name);
        if(clazz == null){
            clazz = Class.forName(name, true, cl);
            NAME_CLASS_CACHE.put(name, clazz);
        }
        return clazz;
    }

    public static ClassLoader getClassLoader(){
        return getClassLoader(PrimitiveUtils.class);
    }

    /**
     * get class loader
     *
     * @param cls
     * @return class loader
     */
    public static ClassLoader getClassLoader(Class<?> cls) {
        ClassLoader cl = null;
        try {
            cl = Thread.currentThread().getContextClassLoader();
        } catch (Throwable ex) {
            // Cannot access thread context ClassLoader - falling back to system class loader...
        }
        if (cl == null) {
            // No thread context class loader -> use class loader of this class.
            cl = cls.getClassLoader();
        }
        return cl;
    }

    private static final Map<String, PrimitiveInfo<?>> PRIMITIVES = new HashMap<String, PrimitiveInfo<?>>();

    static {
        addPrimitive(boolean.class, "Z", Boolean.class, "booleanValue", false);
        addPrimitive(short.class, "S", Short.class, "shortValue", (short) 0);
        addPrimitive(int.class, "I", Integer.class, "intValue", 0);
        addPrimitive(long.class, "J", Long.class, "longValue", 0L);
        addPrimitive(float.class, "F", Float.class, "floatValue", 0F);
        addPrimitive(double.class, "D", Double.class, "doubleValue", 0D);
        addPrimitive(char.class, "C", Character.class, "charValue", '\0');
        addPrimitive(byte.class, "B", Byte.class, "byteValue", (byte) 0);
        addPrimitive(void.class, "V", Void.class, null, null);
    }

    private static <T> void addPrimitive(Class<T> type, String typeCode, Class<T> wrapperType, String unwrapMethod,
                                         T defaultValue) {
        PrimitiveInfo<T> info = new PrimitiveInfo<T>(type, typeCode, wrapperType, unwrapMethod, defaultValue);

        PRIMITIVES.put(type.getName(), info);
        PRIMITIVES.put(wrapperType.getName(), info);
    }

    /** 代表一个primitive类型的信息。 */
    @SuppressWarnings("unused")
    private static class PrimitiveInfo<T> {
        final Class<T> type;
        final String   typeCode;
        final Class<T> wrapperType;
        final String   unwrapMethod;
        final T        defaultValue;

        public PrimitiveInfo(Class<T> type, String typeCode, Class<T> wrapperType, String unwrapMethod, T defaultValue) {
            this.type = type;
            this.typeCode = typeCode;
            this.wrapperType = wrapperType;
            this.unwrapMethod = unwrapMethod;
            this.defaultValue = defaultValue;
        }
    }

    /**
     * 取得primitive类型的wrapper。如果不是primitive，则原样返回。
     * <p>
     * 例如：
     * <p/>
     * <pre>
     * ClassUtil.getPrimitiveWrapperType(int.class) = Integer.class;
     * ClassUtil.getPrimitiveWrapperType(int[].class) = int[].class;
     * ClassUtil.getPrimitiveWrapperType(int[][].class) = int[][].class;
     * ClassUtil.getPrimitiveWrapperType(String[][].class) = String[][].class;
     * </pre>
     * <p/>
     * </p>
     */
    @SuppressWarnings("unchecked")
    public static <T> Class<T> getWrapperTypeIfPrimitive(Class<T> type) {
        if (type.isPrimitive()) {
            return ((PrimitiveInfo<T>) PRIMITIVES.get(type.getName())).wrapperType;
        }

        return type;
    }

    /**
     * 取得primitive类型的默认值。如果不是primitive，则返回<code>null</code>。
     * <p>
     * 例如：
     * <p/>
     * <pre>
     * ClassUtil.getPrimitiveDefaultValue(int.class) = 0;
     * ClassUtil.getPrimitiveDefaultValue(boolean.class) = false;
     * ClassUtil.getPrimitiveDefaultValue(char.class) = '\0';
     * </pre>
     * <p/>
     * </p>
     */
    @SuppressWarnings("unchecked")
    public static <T> T getPrimitiveDefaultValue(Class<T> type) {
        PrimitiveInfo<T> info = (PrimitiveInfo<T>) PRIMITIVES.get(type.getName());

        if (info != null) {
            return info.defaultValue;
        }

        return null;
    }

}

