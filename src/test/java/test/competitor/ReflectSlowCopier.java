package test.competitor;

import io.github.swqxdba.smartcopier.Copier;

import java.beans.BeanInfo;
import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;

public class ReflectSlowCopier implements Copier {

    @Override
    public void copy(Object src, Object target) {
        if (src == null || target == null) {
            return;
        }
        try {
            BeanInfo fromBeanInfo = Introspector.getBeanInfo(src.getClass());
            PropertyDescriptor[] fromProperties = fromBeanInfo.getPropertyDescriptors();
            BeanInfo toBeanInfo = Introspector.getBeanInfo(target.getClass());
            PropertyDescriptor[] toProperties = toBeanInfo.getPropertyDescriptors();
            for (PropertyDescriptor fromPropertyDescriptor : fromProperties) {
                Method fromGetter = fromPropertyDescriptor.getReadMethod();
                if (fromGetter != null) {
                    for (PropertyDescriptor toPropertyDescriptor : toProperties) {
                        if (toPropertyDescriptor.getWriteMethod() != null && toPropertyDescriptor.getName().equals(fromPropertyDescriptor.getName())) {
                            try {
                                Object value = fromGetter.invoke(src);
                                toPropertyDescriptor.getWriteMethod().invoke(target, value);
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void copyNonNullProperties(Object src, Object target) {
        if (src == null || target == null) {
            return;
        }
        try {
            BeanInfo fromBeanInfo = Introspector.getBeanInfo(src.getClass());
            PropertyDescriptor[] fromProperties = fromBeanInfo.getPropertyDescriptors();
            BeanInfo toBeanInfo = Introspector.getBeanInfo(target.getClass());
            PropertyDescriptor[] toProperties = toBeanInfo.getPropertyDescriptors();
            for (PropertyDescriptor fromPropertyDescriptor : fromProperties) {
                Method fromGetter = fromPropertyDescriptor.getReadMethod();
                if (fromGetter != null) {
                    for (PropertyDescriptor toPropertyDescriptor : toProperties) {
                        if (toPropertyDescriptor.getWriteMethod() != null && toPropertyDescriptor.getName().equals(fromPropertyDescriptor.getName())) {
                            try {
                                Object value = fromGetter.invoke(src);
                                if (value != null) {
                                    toPropertyDescriptor.getWriteMethod().invoke(target, value);
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void merge(Object src, Object target) {
        if (src == null || target == null) {
            return;
        }
        try {
            BeanInfo fromBeanInfo = Introspector.getBeanInfo(src.getClass());
            PropertyDescriptor[] fromProperties = fromBeanInfo.getPropertyDescriptors();
            BeanInfo toBeanInfo = Introspector.getBeanInfo(target.getClass());
            PropertyDescriptor[] toProperties = toBeanInfo.getPropertyDescriptors();
            for (PropertyDescriptor fromPropertyDescriptor : fromProperties) {
                Method fromGetter = fromPropertyDescriptor.getReadMethod();
                if (fromGetter != null) {
                    for (PropertyDescriptor toPropertyDescriptor : toProperties) {
                        if (toPropertyDescriptor.getWriteMethod() != null && toPropertyDescriptor.getName().equals(fromPropertyDescriptor.getName())) {
                            try {
                                Object value = fromGetter.invoke(src);
                                Method toGetter = toPropertyDescriptor.getReadMethod();
                                if (value != null && toGetter != null) {
                                    Object currentValue = toGetter.invoke(target);
                                    if (currentValue == null) {
                                        toPropertyDescriptor.getWriteMethod().invoke(target, value);
                                    }
                                }
                            } catch (Exception e) {
                                throw new RuntimeException(e);
                            }
                            break;
                        }
                    }
                }
            }
        } catch (IntrospectionException e) {
            throw new RuntimeException(e);
        }
    }
}
