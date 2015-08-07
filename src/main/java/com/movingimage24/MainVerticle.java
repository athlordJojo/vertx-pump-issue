package com.movingimage24;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.DeploymentOptions;

/**
 * Created by joan on 07/08/15.
 */
public class MainVerticle extends AbstractVerticle{

    @Override
    public void start(io.vertx.core.Future<Void> future) {
        vertx.deployVerticle(DownloadVerticle.class.getCanonicalName(), new DeploymentOptions(), res -> {
            vertx.eventBus().send("downloadFile", null);
        });
    }

}
