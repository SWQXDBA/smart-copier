# Smart Copier

运行时生成bean copier代码的高效率工具。    

使用java assist库在运行时生成代码，兼顾运行效率和开发效率。  
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

## copy
copy方法会用src中的属性给target中的属性直接赋值

## copyNonNullProperties
用src中不为null的属性给target中的属性赋值,类似于部分更新

## merge
用当target中的属性为null时，才用src中的属性进行赋值。


# 设置类
设置类用来在生成Copier实例时，对生成代码的逻辑进行定制。  

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



# debug模式
如果想查看生成的copy方法的方法源码，或者是生成的class字节码:
```
SmartCopier.setDebugGeneratedClassFileDir("your/path/");
SmartCopier.setDebug(true);

```
此时生成的字节码会被输出成class文件，到指定的目录中。