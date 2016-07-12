package com.silencedut.rxjavastudy.rx_imitate;

/**
 * Created by SilenceDut on 16/7/11.
 */

public interface Callback<T> {
    void onResult(T result);
    void onFail(T fail);
}
