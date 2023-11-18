import org.swqxdba.SmartCopier;

import javax.swing.text.html.Option;
import java.util.Optional;

public class Test {
    public void sayHello() {
        SmartCopier.setDebugMode(true);
        SmartCopier.setDebugGeneratedClassFileDir("your/path/");
        System.out.println("hello");
    }

    public static void main(String[] args) {
        ((Test) null).sayHello();
    }
}
