# Smart Copier

运行时生成bean copier代码的高效率工具。    

使用cglib asm库在运行时生成代码，兼顾运行效率和开发效率。  
其性能是BeanUtil的200-1000倍，等同于硬编码。  
比cglib的beanCopier提供了额外的设置功能，如默认值，是否用null覆盖，是否忽视null属性等，以及设置特殊的属性对应关系等等。  

此外，提供更加灵活的转换器，允许只对部分属性进行转换，且不会对其他属性带来额外开销。  
此外，允许提供默认值。  


# 实现原理：

会在运行时生成一个实现类，该实现类实现Copier接口：

```java

public interface Copier {
    /**
     * 强制对发现的所有属性进行拷贝
     * @param src
     * @param target
     */
    void copy(Object src,Object target);

    /**
     * 当源属性不为null时进行拷贝
     * @param src
     * @param target
     */
    void copyNonNullProperties(Object src,Object target);

    /**
     * 合并两个对象，只对目标对象中为null的属性会被更新
     * @param src
     * @param target
     */
    void merge(Object src,Object target);
}


```
# 快速使用
## 1 引入依赖:  
maven 
```
<dependency>
    <groupId>io.github.swqxdba</groupId>
    <artifactId>smart-copier</artifactId>
    <version>0.0.4</version>
</dependency>
```

## 2 使用

```java
import io.github.swqxdba.smartcopier.SmartCopier;

@Data
class Person {

    private String name;

    private int age;
}

@Data
class PersonDto {

    private String name;

    private int age;
}

    public static void main(String[] args) {
        PersonDto personDto; //...
        Person person; //...
        SmartCopier.copy(person, personDto);
    }
```
# 方法介绍


## copy
copy方法会用src中的属性给target中的属性直接赋值

## copyNonNullProperties
用src中不为null的属性给target中的属性赋值,类似于部分更新

## merge
用当target中的属性为null时，才用src中的属性进行赋值。可以理解为合并对象


# 配置类(CopyConfig)
配置类用来在生成Copier实例时，对生成代码的逻辑进行定制。  

注意：CopyConfig对象一旦被用于生成Copier实例，其可能不会被回收，请不要每次都创建新的CopyConfig！！！

注意：CopyConfig中提供的各种接口实现应该是线程安全的!!!

```kotlin
/**
 * 默认值提供者，当源头中数据为null时 将设置defaultValueProvider提供的默认值,defaultValueProvider对每个属性只会被调用一次
 * 注意 如果属性类型是primitive的 则永远不会使用默认值!
 */
interface PropertyValueProvider {

    /**
     *
     * 用于提供默认值 如果提供的默认值不为null 则会被赋值给目标类的该属性
     *
     * 对于copy,copyNonNullProperties和merge方法 可以提供不同的默认值，不会互相影响,
     *
     * 比如在copyNonNullProperties时可以全部提供null表示不使用默认值。
     *
     * @param targetSetter 目标类该属性的setter方法
     * @param sourceGetter 源类该属性的getter方法
     * @param sourceClass 源类型
     * @param sourceClass 目标类型
     * @param copyMethodType 拷贝方法
     * @return 提供的默认值,可以是null 但是必须与targetSetter的参数兼容(可以赋值给该类型的变量)
     */
    fun provide(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>,
        copyMethodType: CopyMethodType
    ): Any?
}

interface PropertyValueConverter {
    /**
     * 是否对该属性使用拦截
     * @param targetSetter 目标类该属性的setter方法
     * @param sourceGetter 源类该属性的getter方法
     * @param sourceClass 源类型
     * @param sourceClass 目标类型
     * @param copyMethodType 拷贝方法
     * @return 是否对该属性使用拦截
     */
    fun shouldIntercept(
        sourceGetter: Method,
        targetSetter: Method,
        sourceClass: Class<*>,
        targetClass: Class<*>,
        copyMethodType: CopyMethodType
    ): Boolean

    /**
     * 转换值
     * @param oldValue 原始值 可能为null
     * @return 结果值 如果属性的类型为primitive类型 则不允许返回null
     */
    fun convert(oldValue: Any?): Any?
}

/**
 * 用于客制化属性之间的对应关系
 */
interface PropertyMapperRuleCustomizer {

    /**
     * 客制化映射规则，会对返回的规则进行检测，如果map.key不是属于sourceClass的方法，或者map.key不是属于targetClass的方法会抛出异常。
     * @param 源类型
     * @param 目标类型
     * @param currentMapper 通过默认规则解析出来的映射关系
     * @return 新的映射关系
     */
    fun mapperRule(
        sourceClass: Class<*>,
        targetClass: Class<*>,
        currentMapper: Map<Method, Method>
    ): Map<Method, Method>
}


/**
 * @param defaultValueProvider 默认值提供者
 * @param propertyValueConverter 属性值转换器
 * @param propertyMapperRuleCustomizer 用于客制化属性的对应关系
 */
class CopyConfig(
    val defaultValueProvider: PropertyValueProvider? = null,
    val propertyValueConverter: PropertyValueConverter? = null,
    val propertyMapperRuleCustomizer: PropertyMapperRuleCustomizer? = null,
)

```
# CopyConfig
CopyConfig提供了三个参数(函数式接口)，当不为null时生效。

# PropertyValueProvider

用于生成默认值，当从src中执行getter方法获取的值为null时，改为使用PropertyValueProvider提供的值。    

默认值允许为null，如果给了一个非null的默认值，那么则当成原属性不为null处理。 

>注意 PropertyValueProvider只在生成Copier实例的时候执行，不会在每次拷贝的时候被执行，
> 执行结果会被作为成员变量存在Copier实例中。
> 



# PropertyValueConverter 
用于值转换 可以在读取src中的属性后进行一层转换再给target的属性赋值。  

如果一个null值经过PropertyValueConverter转换后不再为null，那么则当成原属性不为null处理。  

> 在生成Copier实例的时候，会通过shouldIntercept来判断拷贝过程中要不要对这个属性应用convert，  
> 如果判断要对该属性应用convert，则每次拷贝时该属性都会被应用PropertyValueConverter。
> 
> 即shouldIntercept方法在只在生成Copier实例时调用,PropertyValueConverter在每次拷贝时调用
> 


# PropertyMapperRuleCustomizer 
用于定制属性的映射关系，如把sex属性映射到sexStr属性。   

currentMapper的key是源属性的getter方法，   
currentMapper的value是目标属性的setter方法。  
要求返回的新的map也应该遵循这个规则。并且保证getter返回值和setter参数的类型是兼容的。(比如不能把Integer赋值给String)

> PropertyMapperRuleCustomizer只会在Copier实例生成时被调用，后续拷贝中属性的对应关系是确定的，不会有额外开销。  

#  集合/数组容器的智能兼容处理
在探测属性时 有时候会遇到带泛型的集合，比如:
```java
class Person{
    List<Order> orders1;
    List<Order> orders2;
}
class PersonDto{
    Set<OrderDto> orders;
    List<OrderDto> orders2;
}

```
对于上述两个类型中的orders orders2属性，其中的集合类型或许不同，也或者集合类型兼容，但是集合元素类型不同。  

SmartCopier在遇到集合属性时 会探测集合的具体泛型，避免造成堆污染。简而言之：当集合中元素类型兼容时，将直接拷贝集合元素，
当集合类型不兼容时，将会尝试对其中的每个元素进行转换。  
这个自动的转换逻辑请见后面的BeanConvertProvider配置。  

此外，SmartCopier 支持对集合和数组进行处理，前提是元素类型必须兼容   
(判断方式为class1.isAssignableFrom(class2))  

比如 可以将List&lt;String&gt;转换到  
String[]   
或者Set&lt;String&gt;    
或者HashSet&lt;String&gt;


如果目标容器类型(setter方法参数类型)为具体的实现类 而不是接口 (比如是LinkedList而不是List),  
那么会尝试使用实现类的newInstance去直接构造实现类。

如果目标容器类型为接口或者抽象类型，但是能被赋值为ArrayList 或者HashSet 则会被赋值为ArrayList或者HashSet

此外,还支持元素类型为基本类型的集合/数组 与包装类型的集合转换处理,  
比如 int[] 转换到 List&lt;Integer&gt; 或者Integer[]


### 不支持的转换: (不支持转换时 该属性会被忽略)

List&lt;UserEntity&gt; 到List&lt;UserDto&gt; 因为元素类型不兼容,
且不知道如何进行元素的转换(如果强行赋值会造成堆污染) ，如果想支持 可以通过配置BeanConvertProvider

### 注意： 
如果目标类型为primitive的数组 但是来源元素中有null 那么目标数组中的对应元素会是默认的初始值 比如:
[null,123]->[0,123]

# debug模式
如果想查看生成的copy方法的方法源码，或者是生成的class字节码:
```
SmartCopier.setDebugGeneratedClassFileDir("your/path/");
SmartCopier.setDebug(true);

```
此时生成的字节码会被输出成class文件，到指定的目录中。

# 注意事项

##  为了在高版本java中正常使用 请在jvm参数中允许对其他包的反射

## 在生成Copier时 会根据传入的不同的CopierConfig生成不同的Copier类

请确保CopierConfig重写了合适的hashCode方法,或者使用同一个CopierConfig实例。

## 性能优化
SmartCopier中有静态的copy方法可以调用,但是如果需要更高的性能,请先获取getCopier返回的Copieer实例,
避免每次调用静态方法时反复从读取缓存.

性能较低的做法

```java
import io.github.swqxdba.smartcopier.SmartCopier;

class ProductConverter {

    public void copy(ProductDto dto, Product entity) {
        SmartCopier.copy(dto, entity);
    }
}


```
更高性能的写法

```java
import io.github.swqxdba.smartcopier.SmartCopier;

class ProductConverter {

    Copier copier = SmartCopier.getCopier(ProductDto.class, Product.class);

    public void copy(ProductDto dto, Product entity) {
        copier.copy(dto, entity);
    }
}


```

## 线程安全
SmartCopier本身是线程安全的,包括生成Copier类的过程，以及执行copy的过程。  
但是 如果CopyConfig中传入的PropertyValueConverter等依赖不是线程安全的，那么可能会有线程安全问题。  

## 内存泄漏注意
被传入SmartCopier的CopyConfig对象在程序生命周期中都不会被回收

# 处理不兼容的类型 beanConvertProvider

在属性的拷贝和转换时，SmartCopier默认能够处理那些兼容的类型，但是有时候我们需要对不兼容的类型进行拷贝操作，见以下例子:
```java
class User{}
class UserDto{}
class Order{
    private User user;
}
class OrderDto{
    private UserDto user;
}
```
此时我们想对Order和OrderDto进行拷贝，但是由于User和UserDto不兼容，所以默认情况下是不会进行拷贝的。  


一个简单的想法是：如果遇到项目中定义的类 则当做bean处理，自动调用SmartCopier来拷贝一个副本。

请使用SmartCopier.beanConvertProvider来设置全局的转换器，比如将一个bean转换成另一个bean等等。  
或者进行map 和 bean之间的转换。

以下是一个简单的配置示例，用于将在这个过程中对遇到的com.xxx包中的类进行自动转换：  
```java

SmartCopier.setBeanConvertProvider(new BeanConvertProvider() {
            Map<String, BeanConverter> cache = new ConcurrentHashMap<>();

            @Nullable
            @Override
            public BeanConverter tryGetConverter(@NotNull Class<?> aClass, @NotNull Class<?> aClass1) {
                String key = aClass.getName() + aClass1.getName();
                String basePackage = "com.xxx";//这里写你的项目的包名
                BeanConverter beanConverter = cache.get(key);
                if (beanConverter != null) {
                    return beanConverter;
                }
                if (aClass.getPackage().getName().startsWith(basePackage) && aClass1.getPackage().getName().startsWith(basePackage)) {

                    return cache.computeIfAbsent(key, k -> new BeanConverter() {
                        final Copier copier = SmartCopier.getCopier(aClass, aClass1);

                        final Constructor<?> targetConstructor;

                        {
                            try {
                                targetConstructor = aClass1.getConstructor();
                                targetConstructor.setAccessible(true);
                            } catch (NoSuchMethodException e) {
                                throw new RuntimeException(e);
                            }

                        }

                        @NotNull
                        @Override
                        public Object doConvert(@NotNull Object o) {
                            try {
                                Object instance = targetConstructor.newInstance();
                                copier.copy(o, instance);
                                return instance;
                            } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
                                throw new RuntimeException(e);
                            }
                        }
                    });
                }
                return null;
            }
        });

```
BeanConvertProvider中的tryGetConverter方法用于探测能否找到对应的转换器，如果返回null 则表示无法进行转换。   
这个探测只会在生成具体的Copier类的时候被调用，不会在每次copy调用的时候被调用，因此不做缓存也可以。  

## 处理Map到普通对象的转换
同上个例子
```java
class User{}
class UserDto{}
class Order{
    private Map user;
}
class OrderDto{
    private UserDto user;
}
```
假设你想将遇到的Map转换到bean 或者反过来，也只需要在SmartCopier.setBeanConvertProvider提供的BeanConvertProvider中支持即可。

## 递归调用
在定义beanConvertProvider时 我们可能会遇到递归的结构
```java
class Person{
    Person p1;

}
class PersonDto{
    PersonDto p1;
}
```
对于上述结构 当尝试将Person转换到PersonDto时 发现需要生成Person->PersonDto的copier  

在生成Person->PersonDto的copier时 由于属性类型p1不兼容，调用了设置的beanConvertProvider。  

接下来 beanConvertProvider中尝试生成一个为p1属性进行转换的Copier 这个Copier也是Person->PersonDto。  

因此，在上述场景中，会在生成Copier的过程中需要生成相同功能的另一个Copier，造成死递归。  

请放心，上述问题不会发生，SmartCopier使用了一个简单的二级缓存来解决这个问题，您可以放心地在生成Copier的过程中，通过beanConvertProvider去生成另一个Copier。  
