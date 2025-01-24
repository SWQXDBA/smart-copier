# Smart Copier
SmartCopier是一个用于高效处理Bean拷贝、转换的工具。支持复杂嵌套的同构或异构的类型结构。 

其希望达成的效果类似于Jackson的 objectMapper.convertValue(src,targetType),即类似于把一个对象转换成json，然后反序列化到另一个类型那样通用。  


使用cglib asm库在运行时生成转换对象的字节码，兼顾运行效率和开发效率。    
其性能是BeanUtil.copyProperties的200-1000倍，等同于硬编码。    

比cglib的beanCopier提供了额外的设置功能，如默认值，是否用null覆盖，是否忽视null属性等，以及设置特殊的属性对应关系等等。  

此外，提供更加灵活的转换器，允许只对部分属性进行转换，且不会对其他属性带来额外开销。

项目使用kotlin编写，支持java与kotlin项目。

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
# 可用性
本项目有一些基本的测试用例覆盖，在多个生产环境项目中使用。对于大多数场景应该可以提供可靠的支持，包括基于Lombok的java项目等。  

支持java8及之后的jdk版本，在java8、17、21上均有项目运行


# 快速使用
## 1 引入依赖:  
maven 
```
<dependency>
    <groupId>io.github.swqxdba</groupId>
    <artifactId>smart-copier</artifactId>
    <version>0.1.1</version>
</dependency>
```

## 2 使用

```java

static SmartCopier smartCopier = new SmartCopier();
public static void main(String[] args) {
    PersonDto personDto; //...
    Person person; //...
    smartCopier.copy(person, personDto);
}
```

# 详细介绍
## Copier

Copier是一个接口，定义了三个方法：

```kotlin
interface Copier {
    /**
     * 强制对发现的所有属性进行拷贝
     * @param src
     * @param target
     */
    fun copy(src: Any?, target: Any?)

    /**
     * 当源属性不为null时进行拷贝
     * 如果源属性为null 但是经过了转换后的目标属性不为null 也会执行更新。
     * 如果源属性为null 但是提供了一个非null的默认值，也可能会执行更新
     * @param src
     * @param target
     */
    fun copyNonNullProperties(src: Any?, target: Any?)

    /**
     * 合并两个对象，只对目标对象中为null的属性会被更新
     * 如果某个属性为primitive的 则不会再被merge更新
     * @param src
     * @param target
     */
    fun merge(src: Any?, target: Any?)
}
```

SmartCopier会在运行时，为要拷贝的类型生成对应的Copier实现类。 

可以通过SmartCopier.getCopier获取到内部生成的Copier。
多次调用SmartCopier.getCopier会返回同一个Copier对象。Copier是线程安全的。

```java

import io.github.swqxdba.smartcopier.SmartCopier;

static SmartCopier smartCopier = new SmartCopier();
static Copier copier = smartCopier.getCopier(A.class,B.class);

public static void copy(A a , B b) {
    copier.copyNonNullProperties(a,b);
}

```

## 配置类(CopyConfig)
配置类用来在生成Copier实例时，对生成代码的逻辑进行定制。  
你可以为SmartCopier设置默认的CopyConfig，也可以在每次getCopier时传入CopyConfig。

```java
void example(){
    SmartCopier smartCopier = new SmartCopier();
    CopyConfig config = //xxx
    smartCopier.setDefaultConfig(config);
    smartCopier.getCopier(A.class,B.class);
}

void example(){
    SmartCopier smartCopier = new SmartCopier();
    CopyConfig config = //xxx
    smartCopier.getCopier(A.class,B.class,config);
}
```
请注意，使用不同的config时生成的Copier并不一样  
以下三个Copier对象是不同的。
```java
void example(){
    SmartCopier smartCopier = new SmartCopier();
    CopyConfig config1 = //xxx
    CopyConfig config2 = //xxx
    Copier copier1 = smartCopier.getCopier(A.class,B.class,config);
    Copier copier2 = smartCopier.getCopier(A.class,B.class,config2);
    Copier copier3 = smartCopier.getCopier(A.class,B.class);
}
```

## SmartCopier类
SmartCopier类是一个Copier的工厂入口，负责生成Copier。不同的SmartCopier生成的Copier不一样。    

通常情况下，一个应用只要一个SmartCopier实例即可。    

### 让SmartCopier类输出生成的类
你可以让SmartCopier把运行时生成的Copier实例的字节码输出到指定的路径中。  

1 启动debug模式  
smartCopier.setDebugMode(true);  

2 设置输出的class文件路径  
smartCopier.setDebugOutPutDir("./");  

3 触发copier生成  
smartCopier.getCopier(A.class,B.class);

然后你就能在那个路径下看到生成的.class文件，你可以用idea直接打开它来查看其反编译的java代码。  

### SmartCopier类快捷方法
你可以直接通过SmartCopier实例执行常用的拷贝方法，而无需获取内部的Copier实例。  

```java
void example(A a,B b){
    SmartCopier smartCopier = new SmartCopier();
    smartCopier.copy(a,b);
    //等同于
    Copier copier = smartCopier.getCopier(A.class,B.class);
    copier.copy(a,b);
}

List<B> example2(List<A> list){
    SmartCopier smartCopier = new SmartCopier();
    return smartCopier.copyToList(list,B.class);
}

```
在内部，会通过一个ConcurrentHashMap来获取生成的Copier实例执行拷贝。

## Bean
SmartCopier是Bean属性拷贝和转换的工具，依靠Getter、Setter来寻找类中的属性。  
除了普通的set get方法外，还会识别出带有返回值的set方法。

```java
A setName(String name){
    this.name=name;
    return this;
}
```

## 类型转换
在拷贝/转换的过程中，会遇到不一样类型的同名属性，比如:

```java
class Dto{
    List<SubDto> children;
}

class Entity{
    List<SubEntity> children;
}
```
在一些BeanUtil中，只使用class.isAssignableFrom来判断是否能赋值。 但是这么判断不会判断集合元素的内部类型，会造成堆污染。  
SmartCopier会执行递归检测，以保证其中的泛型实参也能够互相兼容。如果不兼容，则需要提供转换器进行转换处理，否则不会对该属性进行转换。  

在上面的例子中，需要提供一个类型转换器，提供SubDto和SubEntity的转换逻辑。  
你可以通过CopyConfig来配置你的转换器。
```java

CopyConfig config = new CopyConfig();
config.addConverter(yourTypeConverterProvider);

```
### TypeConverterProvider
TypeConverterProvider是一个转换器工厂，用于生成转换器实例。每个转换器负责从一个类型到另一个类型的转换。 

而TypeConverterProvider负责生成这些转换器。这么设计的好处是，你可以在转换器对象内部保存一些状态。  

一个常见的类型转换器是PackageBasedTypeConverterProvider，  它使用一个SmartCopier来对某个包名下的所有类型进行转换。  

你可以把项目所在的包名传进去，让其转换项目中的各种类型。  

```java
import io.github.swqxdba.smartcopier.CopyConfig;
import io.github.swqxdba.smartcopier.SmartCopier;

@Bean
public SmartCopier smartCopier() {
    SmartCopier smartCopier = new SmartCopier();
    CopyConfig copyConfig = new CopyConfig();
    copyConfig.addConverter(new PackageBasedTypeConverterProvider("com.company.project"),smartCopier);
    smartCopier.setDefaultConfig(copyConfig);
    return smartCopier;
}


```

### 拆装箱

SmartCopier默认使用BoxTypeConverterProvider进行自动拆装箱，在拆箱时，如果遇到为null的包装类，会返回默认值。

## 集合与数组
集合和数组的转换不好处理，SmartCopier内置了一个ContainerTypeConverterProvider用于处理集合与数组的相关转换。  
这个转换器是默认启用的，你可以在CopyConfig中移除掉这个内置的转换器，来取消对集合类型的自动转换。  

集合自动转换指的是类似于以下的几种类型间的互相转换，不包括Map的转换。  
```java

class A{
    List<Integer> list;
}
class B{
    Set<Integer> list;
}
class C{
    Integer[] list;
}
class D{
    int[] list;
}
class D{
    Collection<Integer> list;
}
```

```
注意 当你想把一个List<Integer>转换到int[]，且前面的List中存在null元素时，相应的位置会是元素的默认值。  
比如有 List<Integer> = [1,2,null,4]
转换到int[]后会变成 [1,2,0,4]
```
> SmartCopier指的基础类型默认值，是基础类型数组元素的默认值，比如 (new int[1])[0]


如果集合元素非基础类型或者包装类，而是自定义的Bean，那么SmartCopier会尝试询问注册的TypeConverterProvider来获取一个转换器来进行转换  