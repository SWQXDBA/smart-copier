import org.swqxdba.Copier;
import org.swqxdba.PropertyValueConverter;

public class BeanCopierTestImpl implements Copier {
    Person sourceClass;
    Animal targetClass;
    PropertyValueConverter generatedField1;

    public void copyInternal(Person var1, Animal var2) {
        int var3 = var1.getAge();
        var2.setAge(var3);
        String var4 = var1.getName();
        if (var4 != null) {
            var2.setName(var4);
        }

    }

    public void copy(Object var1, Object var2) {
        this.copyInternal((Person)var1, (Animal)var2);
    }

}