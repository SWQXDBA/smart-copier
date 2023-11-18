import javax.swing.text.html.Option;
import java.util.Optional;

public class Test {
    public void sayHello() {
        System.out.println("hello");
    }

    public static void main(String[] args) {
        ((Test) null).sayHello();
    }
}
