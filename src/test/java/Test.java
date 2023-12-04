import org.swqxdba.SmartCopier;

import javax.swing.text.html.Option;
import java.util.Optional;

public class Test {


    public void sayHello() {
        SmartCopier.setDebugMode(true);
        SmartCopier.setDebugGeneratedClassFileDir("your/path/");
        System.out.println("hello");
    }


    public static  void copy(java.lang.Object srcBean, java.lang.Object targetBean) {
        Person src = (Person) srcBean;
        Animal target = (Animal) targetBean;
        {
            int tempValue = src.getAge();
            target.setAge(tempValue);
        }
        {
            int[] tempValue = src.getArray();
            target.setArray(tempValue);
        }
        {
            int[][] tempValue = src.getArray2();
            target.setArray2(tempValue);
        }
        {
            java.lang.Object[] tempValue = (java.lang.Object[]) src.getArray3();
            target.setArray3(tempValue);
        }
        {
            java.util.List tempValue = src.getGenericType();
            target.setGenericType(tempValue);
        }
        {
            java.util.List tempValue = src.getList1();
            target.setList1(tempValue);
        }
        {
            java.lang.String tempValue = src.getName();
            target.setName(tempValue);
        }
        {
            java.lang.String[] tempValue = src.getTypeArr();
            target.setTypeArr(tempValue);
        }
    }

}
