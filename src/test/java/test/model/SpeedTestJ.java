package test.model;

import cn.hutool.core.bean.BeanUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Data;
import lombok.SneakyThrows;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.swqxdba.smartconvert.Copier;
import org.swqxdba.smartconvert.SmartCopier;
import org.swqxdba.smartconvert.bean.BeanConvertProvider;
import org.swqxdba.smartconvert.bean.BeanConverter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class SpeedTestJ {

    @Data
    public static class MockData{
        Integer i1;
        Long l1;

        long l2;

        Short s1;

        short s2;

        MockData son;

        String string1;

        List<String> strings = new ArrayList<>();

        Integer[] ints = new Integer[0];

    }

    @Data
    public static class MockData2{
        Integer i1;
        Long l1;

        long l2;

        Short s1;

        short s2;

        MockData2 son;

        String string1;

        String[] strings = new String[0];

        int[] ints;
    }
    static List<MockData> mockDataList = new ArrayList<>();
    static MockData2 holder = new MockData2();
    static Copier copier = SmartCopier.getCopier(MockData.class,MockData2.class);

    static{
        for (int i = 0; i < 100; i++) {
            MockData mockData = new MockData();
            mockData.i1 = i;
            mockData.l1 = (long)i;
            mockData.l2 = i;
            mockData.s1 = (short)i;
            mockData.s2 = (short)i;
            mockData.son = new MockData();
            mockData.son.i1 = i;
            mockData.son.l1 = (long)i;
            mockData.son.l2 = i;
            mockData.son.s1 = (short)i;
            mockData.son.s2 = (short)i;
            mockData.string1 = "string"+i;
            mockData.strings = new ArrayList<>();
            for (int j = 0; j < 10; j++) {
                mockData.strings.add("string"+i+j);
            }
            mockData.ints = new Integer[]{i,1,3,5,5,1,6,32,4,1};
            mockDataList.add(mockData);
        }
        SmartCopier.setBeanConvertProvider(new BeanConvertProvider() {
            @NotNull
            @Override
            public BeanConverter tryGetConverter(@NotNull Class<?> fromClass, @NotNull Class<?> toClass) {
                Copier copier1 = SmartCopier.getCopier(fromClass, toClass);
                return  new BeanConverter() {

                    @NotNull
                    @Override
                    public Object doConvert(@NotNull Object from) {
                        try {
                            Object instance = toClass.newInstance();
                            copier1.copy(from,instance);
                            return instance;
                        } catch (InstantiationException | IllegalAccessException e) {
                            throw new RuntimeException(e);
                        }
                    }
                };
            }
        });
    }
    static int rounds = 200;

    private static void hardcodeCopy(MockData from, MockData2 to){
        // Copy primitive and wrapper types
        to.setI1(from.getI1());
        to.setL1(from.getL1());
        to.setL2(from.getL2());
        to.setS1(from.getS1());
        to.setS2(from.getS2());
        to.setString1(from.getString1());

        // Copy arrays
        if(from.getInts()!=null){
            int length = from.getInts().length;
            int[] ints = new int[length];
            for (int i = 0; i < length; i++) {
                ints[i] = from.getInts()[i];
            }
            to.setInts(ints);
        }


        // Copy List to array
        List<String> fromStrings = from.getStrings();
        String[] toStrings = new String[fromStrings.size()];
        for (int i = 0; i < fromStrings.size(); i++) {
            toStrings[i] = fromStrings.get(i);
        }
        to.setStrings(toStrings);

        // Handle recursive copy for nested objects
        if (from.getSon() != null) {
            MockData2 targetSon = new MockData2();
            hardcodeCopy(from.getSon(), targetSon);
            to.setSon(targetSon);
        }
    }
    private static void beanUtilCopy(MockData from, MockData2 to){
        BeanUtil.copyProperties(from,to);
    }

    private static void smartCopierCopy(MockData from, MockData2 to){
        copier.copy(from,to);
    }

    public static void main(String[] args) {
        TestCopy handle = SpeedTestJ::hardcodeCopy;
        TestCopy beanUtil = SpeedTestJ::beanUtilCopy;
        TestCopy smartCopier = SpeedTestJ::smartCopierCopy;

        for (int i = 0; i < 10; i++) {
            System.out.println("smartCopier拷贝:");
            doTest(smartCopier);

            System.out.println("硬编码拷贝:");
            doTest(handle);

//            System.out.println("beanUtil拷贝:");
//            doTest(beanUtil);
            System.out.println("\n\n");
        }
    }


    interface TestCopy{
        void copy(MockData m1, MockData2 m2);
    }
    private static void doTest(TestCopy testCopy){
        long start = System.currentTimeMillis();
        for (int i = 0; i < rounds; i++) {
            for (MockData mockData : mockDataList) {
                testCopy.copy(mockData,holder);
            }
        }
        System.out.println("耗时 "+(System.currentTimeMillis()-start)+" mills");
    }
}
