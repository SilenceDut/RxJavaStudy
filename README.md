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
                }, new Action1<Throwable>() {
                    @Override
                    public void call(Throwable throwable) {
                        NewsEvent newsEvent= new NewsEvent(new News(), Constant.GETNEWSWAY.INIT,newsType);
                        newsEvent.setEventResult(Constant.Result.FAIL);
                        AppService.getBus().post(newsEvent);
                    }
                });

```
以[**NBAPlus**](https://github.com/SilenceDut/NBAPlus/blob/master/app/src/main/java/com/me/silencedut/nbaplus/rxmethod/RxNews.java)里的代码为例,这里的News泛型限定符代表发出的事件类型是News类型,
在subscribe(new Action1<News>(){}}的过程中,这里的News与上面对应是处理News类型的信息,首先将Action1转化为一个Subscriber对象,这里转化方法类似[**AdapterPattern(适配器模式)**](https://github.com/SilenceDut/DesignPatterns/blob/master/src/com/silencedut/structural_patterns/adapter/design_rules.md)
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
所以可以看出只有当有Action1也就是Subscriber(Observer)注册了Observable才会触发OnSubscribe的call方法,并传入此观察者Subscriber的对象,此时可在call方法中对此观察者Subscriber的进行处理