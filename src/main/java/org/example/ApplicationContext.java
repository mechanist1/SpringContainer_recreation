package org.example;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;

public class ApplicationContext {
    private HashMap<String,Object> map=new HashMap<>();

    public <T> T getBean(String name){
        Object o = map.get(name);
        if(o!=null){
            return (T) o;
        }
        throw new IllegalArgumentException("Bean not found "+name);
    }

    public void defineBean(Class clazz) throws IllegalAccessException, InvocationTargetException {
        String n = resolveName(clazz.getSimpleName());
        Object o = map.get(n);
        if(o!=null){
            return;
        }
        if(clazz.getAnnotation(Component.class)!=null){
            o=newBeanInstance(clazz);
            map.put(n,o);
            injectFields(o);
            initializePostConstruct(o);
            prepareBeanMethods(o);
        }
    }

    private void prepareBeanMethods(Object o) throws InvocationTargetException, IllegalAccessException {
        Class<?> c = o.getClass();
        while(c!=null){
            for (Method m : c.getDeclaredMethods()) {
                if(m.getAnnotation(Bean.class)!=null){
                    Object r=m.invoke(o);
                    map.put(m.getName(),r);
                }
            }
            c=c.getSuperclass();
        }
    }

    private void initializePostConstruct(Object o) throws InvocationTargetException, IllegalAccessException {
        Class<?> c = o.getClass();
        while(c!=null){
            for (Method m : c.getDeclaredMethods()) {
                if(m.getAnnotation(PostConstruct.class)!=null){
                    m.invoke(o);
                }
            }
            c=c.getSuperclass();
        }
    }

    private void injectFields(Object o) throws IllegalAccessException {
        Class c=o.getClass();
        while(c!=null){
            for (Field f : c.getDeclaredFields()){
                if(f.getAnnotation(Autowired.class)!=null){
                    f.setAccessible(true);
                    f.set(o,getBean(f.getType()));
                }
            }
            c=c.getSuperclass();
        }
    }

    private Object newBeanInstance(Class clazz) {
        Constructor c = null;
        try {
            c = clazz.getConstructor();
            c.setAccessible(true);
            return c.newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public <T> T getBean(Class<T> clazz){
        String n = clazz.getSimpleName();
        String s = resolveName(n);
        return getBean(s);
    }

    private static String resolveName(String n) {
        return Character.toLowerCase(n.charAt(0))
                + n.substring(1);
    }
}
