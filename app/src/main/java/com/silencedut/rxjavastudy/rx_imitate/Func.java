package com.silencedut.rxjavastudy.rx_imitate;

/**
 * Created by SilenceDut on 16/7/11.
 */

public interface Func<T, R> {
    R call(T t);
}