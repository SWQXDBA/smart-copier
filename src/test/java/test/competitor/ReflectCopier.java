package test.competitor;

import io.github.swqxdba.smartcopier.Copier;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ReflectCopier implements Copier {

    Class<?> from;

    Class<?> to;

    Method[] fromGetters;

    Method[] toSetters;

    Method[] toGetters;

    public ReflectCopier(Class<?> from, Class<?> to) {
        this.from = from;
        this.to = to;
        try {
            BeanInfo beanInfo = Introspector.getBeanInfo(from);
            PropertyDescriptor[] propertyDescriptors = beanInfo.getPropertyDescriptors();
            BeanInfo toBeanInfo = Introspector.getBeanInfo(to);
            PropertyDescriptor[] toProperties = toBeanInfo.getPropertyDescriptors();
            Map<String, PropertyDescriptor> toPropertyMap = new HashMap<>();
            List<Method> toSetterList = new ArrayList<>();
            List<Method> fromGetterList = new ArrayList<>();
            List<Method> toGetterList = new ArrayList<>();
            for (PropertyDescriptor propertyDescriptor : toProperties) {
                if(propertyDescriptor.getWriteMethod()!= null){
                    toPropertyMap.put(propertyDescriptor.getName(), propertyDescriptor);
                }
            }
            for (PropertyDescriptor propertyDescriptor : propertyDescriptors) {
                Method readMethod = propertyDescriptor.getReadMethod();
                if(readMethod != null){
                    PropertyDescriptor writePropertyDescriptor = toPropertyMap.get(propertyDescriptor.getName());
                    Method writeMethod = writePropertyDescriptor.getWriteMethod();
                    if(writeMethod != null){
                        toSetterList.add(writeMethod);
                        fromGetterList.add(readMethod);
                        readMethod.setAccessible(true);
                        writeMethod.setAccessible(true);
                        Method readMethod1 = writePropertyDescriptor.getReadMethod();
                        if(readMethod1 != null){
                            readMethod1.setAccessible(true);
                            toGetterList.add(readMethod1);
                        }else{
                            toGetterList.add(null);
                        }
                    }
                }
            }
            this.fromGetters = fromGetterList.toArray(new Method[0]);
            this.toSetters = toSetterList.toArray(new Method[0]);
            this.toGetters = toGetterList.toArray(new Method[0]);

        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
    @Override
    public void copy(Object src, Object target) {
        if(src == null || target == null){
            return;
        }
        for (int i = 0; i < fromGetters.length; i++) {
            try {
                Object value = fromGetters[i].invoke(src);
                toSetters[i].invoke(target, value);
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }

    }

    @Override
    public void copyNonNullProperties(Object src, Object target) {

        if(src == null || target == null){
            return;
        }
        for (int i = 0; i < fromGetters.length; i++) {
            try {
                Object value = fromGetters[i].invoke(src);
                if(value != null){
                    toSetters[i].invoke(target, value);
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    @Override
    public void merge(Object src, Object target) {

        if(src == null || target == null){
            return;
        }
        for (int i = 0; i < fromGetters.length; i++) {
            try {
                Object value = fromGetters[i].invoke(src);
                Method toGetter = toGetters[i];
                if(value != null&&toGetter!=null){
                    Object currentValue = toGetter.invoke(target);
                    if(currentValue == null){
                        toSetters[i].invoke(target, value);
                    }
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }
    }
}
