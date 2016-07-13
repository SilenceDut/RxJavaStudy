# RxJava
##Observable 的创建以及调用过程
create方式创建
![image](https://github.com/SilenceDut/RxJavaStudy/blob/master/image/create.png)
```java 
Observable.create(new Observable.OnSubscribe<News>() {
                    @Override
                    public void call(Subscriber<? super News> subscriber) {
                        News news = getCacheNews(newsType);
                        subscriber.onNext(news);
                        subscriber.onCompleted();
                    }
                }).subscribeOn(Schedulers.io())
                .subscribe(new Action1<News>() {
                    @Override
                    public void call(News news) {
                        NewsEvent newsEvent= new NewsEvent(news, Constant.GETNEWSWAY.INIT,newsType);
                        if(news==null) {
                            newsEvent.setEventResult(Constant.Result.FAIL);
                        }
                        AppService.getBus().post(newsEvent);
                    }
                });

```
以[**NBAPlus**](https://github.com/SilenceDut/NBAPlus/blob/master/app/src/main/java/com/me/silencedut/nbaplus/rxmethod/RxNews.java)里的代码为例,这里的News泛型限定符代表发出的事件类型是News类型,
在subscribe(new Action1<News>(){}}的过程中,这里的News与上面对应是处理News类型的信息,首先将Action1转化为一个Subscriber对象,这里转化方法类似[**AdapterPattern(适配器模式)**](https://github.com/SilenceDut/DesignPatterns/blob/master/src/com/silencedut/structural_patterns/adapter/design_rules.md)
,因此可以理解为表面上是subscribe一个Action1对象,但最终会转化为Subscriber(即Observer)对象进行注册。因为Java不支持函数作为参数,因此 Action1 将 onNext(obj) 和 onError(error) 打包起来传入 subscribe() 以实现不完整定义的回调。
```java 
    public final Subscription subscribe(final Action1<? super T> onNext, final Action1<Throwable> onError) {
        if (onNext == null) {
            throw new IllegalArgumentException("onNext can not be null");
        }
        if (onError == null) {
            throw new IllegalArgumentException("onError can not be null");
        }

        Action0 onCompleted = Actions.empty();
        return subscribe(new ActionSubscriber<T>(onNext, onError, onCompleted));
    }
    
    
    public final class ActionSubscriber<T> extends Subscriber<T> {
    
        final Action1<? super T> onNext;
        final Action1<Throwable> onError;
        final Action0 onCompleted;
    
        public ActionSubscriber(Action1<? super T> onNext, Action1<Throwable> onError, Action0 onCompleted) {
            this.onNext = onNext;
            this.onError = onError;
            this.onCompleted = onCompleted;
        }
    
        @Override
        public void onNext(T t) {
            onNext.call(t);
        }
    
        @Override
        public void onError(Throwable e) {
            onError.call(e);
        }
    
        @Override
        public void onCompleted() {
            onCompleted.call();
        }
    }
```
而在Observable的subscribe中,经简化可以
```
onSubscribe.call(subscriber);
```
所以可以看出只有当有Action1也就是Subscriber(Observer)注册了Observable才会触发OnSubscribe的call方法,并传入此观
察者Subscriber的对象,此时可在call方法中对此观察者Subscriber的进行处理

##lift()变换
```
//可以将lift()变换的源码整理如下
public <R> Observable<R> lift(Operator<? extends R, ? super T> operator) {
    return new Observable(new OnSubscribeLift<T, R>(onSubscribe, operator) {
        @Override
        public void call(Subscriber subscriber) {
            Subscriber newSubscriber = operator.call(subscriber);
            newSubscriber.onStart();
            onSubscribe.call(newSubscriber);
        }
    });
}
```
通过源码可以看出每次操作符的变换都会产生一个新的Observable,OnSubscribe和新的Subscriber,这就是函数式编程不修改状态,没用
副作用（函数式编程只是返回新的值，不修改系统变量。因此，不修改变量，也是它的一个重要特点）。这里的onSubscribe变量是最初创建
流的那个OnSubscribe,匿名内部类OnSubscribeLift就是在注册(subscribe)时接收观察者Subscriber的,newSubscriber就是接受
Subscriber产生的新的Subscriber（这个过程其实就是在新建一个Subscriber,然后在其onNext()（onError,onComplete()等）中
加入一些新的东西,比如doOnNext(Action onNext)操作符时先调用onNext.call(),在调用自身的传入的subscribe.onNext()）,因此
调用的流程也清晰了,如果进行了变换,在注册时会调用新的Observable的OnSubscribeLift的call(Subscriber subscriber) 方法,
在其中将Subscriber通过操作符变换产生新的Subscriber,然后用在调用最初的产生数据的Observable的Subscriber的call方法,
**注意,到目前为止OnNext方法一直未被调用,事件未真正的开始发射通知。这只是注册链。**onSubscribe.call(subscriber)里的
subscriber.onNext()被调用才是真正的开始发射数据。

通过一些常用的变换来理解,主要分析Operator的call()方法。
###doOnNext(new Action1 {..call().} )
```
    Observable.just("Hello, world!") 
    .doOnNext(new Action1<Person>() {
            @Override
            public void call(Person person) {
                person.age = 301;
            }
        })
    .subscribe(i -> System.out.println(i)); 

    /*
    *源码整理去除非相关部分的代码
    */
    onNextObserver = new ActionSubscriber(onNext); // 将Action1转化为Observer,主要是用onNext接口方法替代call接口,类似适配器
    public Subscriber<? super T> call(final Subscriber<? super T> observer) {
        return new Subscriber<T>(observer) {
            @Override
            public void onNext(T value) {
                onNextObserver.onNext(value);
                observer.onNext(value);
            }
        }
```
###map(new Fun1<T,R> {..call(R).} ),根据函数关系将类型T,装化为类型R,
```

    Observable.just("Hello, world!")  
        .map(new Func1<String, Integer>() {  
            @Override  
            public Integer call(String s) {  
                return s.hashCode();  
            }  
        })  
    .subscribe(i -> System.out.println(Integer.toString(i))); 
    
    
    /*
    *源码整理去除非相关部分的代码
    */
    Func1 mapper ; //
    public final <R> Observable<R> map(Func1<? super T, ? extends R> func) {
            return lift(new OperatorMap<T, R>(func));
    }

     public final class OperatorMap<T, R> implements Operator<R, T>  {
        public Subscriber<? super T> call(final Subscriber<? super T> observer) {
            return new Subscriber<T>(observer) { //新的Subscriber会先转化,再传值
                @Override
                public void onNext(T value) {
                    result = func.call(value);  //先根据传入的Func1函数转化传入的value值
                    observer.onNext(result);    //然后将结果传给Subscriber
                }
            }
        
    }
```