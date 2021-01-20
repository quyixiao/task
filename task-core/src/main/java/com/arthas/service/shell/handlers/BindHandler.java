package com.arthas.service.shell.handlers;

import com.arthas.service.shell.future.Future;
import com.arthas.service.utils.LogUtil;
import com.taobao.middleware.logger.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * @author ralf0131 2017-04-24 18:23.
 */
public class BindHandler implements Handler<Future<Void>> {

    private static final Logger logger = LogUtil.getArthasLogger();

    private AtomicBoolean isBindRef;

    public BindHandler(AtomicBoolean isBindRef) {
        this.isBindRef = isBindRef;
    }

    @Override
    public void handle(Future<Void> event) {
        if (event.failed()) {
            logger.error(null, "Error listening term server:", event.cause());
            isBindRef.compareAndSet(true, false);
        }
    }
}
