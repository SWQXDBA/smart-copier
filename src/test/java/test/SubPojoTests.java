package test;

import io.github.swqxdba.smartcopier.converter.TypeConverterProvider;
import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.swqxdba.smartcopier.SmartCopier;
import io.github.swqxdba.smartcopier.converter.TypeConverter;

import java.lang.reflect.Type;

public class SubPojoTests {

    @Data
    public static class Fa{
        Fa f1;
    }
    @Data
    public static class Fa2{

        Fa2 f1;
    }

    private SmartCopier SmartCopier = new SmartCopier();
    @Test
    public void doTest(){
        SmartCopier.getDefaultConfig().addConverter(new TypeConverterProvider() {
            @Nullable
            @Override
            public TypeConverter tryGetConverter(@NotNull Type from, @NotNull Type to) {
                return new TypeConverter() {
                    @Override
                    public Object doConvert( Object from) {
                        try {
                            Object instance = ((Class<?>)to).newInstance();
                            SmartCopier.copy(from,instance);
                            return instance;
                        } catch (InstantiationException e) {
                            throw new RuntimeException(e);
                        } catch (IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }


        });
        Fa from = new Fa();
        from.f1 = new Fa();
        Fa2 to = new Fa2();
        SmartCopier.copy(from,to);
        Assertions.assertNotNull(to.getF1());
    }
}
