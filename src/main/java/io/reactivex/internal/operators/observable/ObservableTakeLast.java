/**
 * Copyright 2016 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License is
 * distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See
 * the License for the specific language governing permissions and limitations under the License.
 */

package io.reactivex.internal.operators.observable;

import java.util.ArrayDeque;

import io.reactivex.*;
import io.reactivex.disposables.Disposable;
import io.reactivex.internal.disposables.DisposableHelper;

public final class ObservableTakeLast<T> extends ObservableWithUpstream<T, T> {
    final int count;
    
    public ObservableTakeLast(ObservableConsumable<T> source, int count) {
        super(source);
        this.count = count;
    }

    @Override
    public void subscribeActual(Observer<? super T> t) {
        source.subscribe(new TakeLastSubscriber<T>(t, count));
    }
    
    static final class TakeLastSubscriber<T> extends ArrayDeque<T> implements Observer<T>, Disposable {
        /** */
        private static final long serialVersionUID = 7240042530241604978L;
        final Observer<? super T> actual;
        final int count;
        
        Disposable s;
        
        volatile boolean done;
        volatile boolean cancelled;
        
        public TakeLastSubscriber(Observer<? super T> actual, int count) {
            this.actual = actual;
            this.count = count;
        }
        
        @Override
        public void onSubscribe(Disposable s) {
            if (DisposableHelper.validate(this.s, s)) {
                this.s = s;
                actual.onSubscribe(this);
            }
        }
        
        @Override
        public void onNext(T t) {
            if (count == size()) {
                poll();
            }
            offer(t);
        }
        
        @Override
        public void onError(Throwable t) {
            actual.onError(t);
        }
        
        @Override
        public void onComplete() {
            done = true;
            if (cancelled) {
                return;
            }
            Observer<? super T> a = actual;
            for (;;) {
                if (cancelled) {
                    return;
                }
                T v = poll();
                if (v == null) {
                    if (!cancelled) {
                        a.onComplete();
                    }
                    return;
                }
                a.onNext(v);
            }
        }
        
        @Override
        public void dispose() {
            if (!cancelled) {
                cancelled = true;
                s.dispose();
            }
        }

        @Override
        public boolean isDisposed() {
            return cancelled;
        }
    }
}
