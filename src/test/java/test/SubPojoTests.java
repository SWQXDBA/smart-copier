package test;

import lombok.Data;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import io.github.swqxdba.smartcopier.SmartCopier;
import io.github.swqxdba.smartcopier.typeconverter.TypeConvertProvider;
import io.github.swqxdba.smartcopier.typeconverter.TypeConverter;

public class SubPojoTests {

    @Data
    public static class Fa{
        Fa f1;
    }
    @Data
    public static class Fa2{

        Fa2 f1;
    }


    @Test
    public void doTest(){
        SmartCopier.setTypeConvertProvider(new TypeConvertProvider() {
            @Nullable
            @Override
            public TypeConverter tryGetConverter(@NotNull Class<?> from, @NotNull Class<?> to) {
                return new TypeConverter() {
                    @NotNull
                    @Override
                    public Object doConvert(@NotNull Object from) {
                        try {
                            Object instance = to.newInstance();
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
        SmartCopier.setTypeConvertProvider(null);
    }
}
