package com.movingimage24;

import io.vertx.core.AbstractVerticle;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.core.eventbus.Message;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.http.HttpClient;
import io.vertx.core.http.HttpClientOptions;
import io.vertx.core.http.HttpClientRequest;
import io.vertx.core.http.HttpClientResponse;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;

import java.io.File;
import java.net.URL;
import java.util.UUID;

/**
 * Created by joan on 07/08/15.
 */
public class DownloadVerticle extends AbstractVerticle {

    private Logger logger = LoggerFactory.getLogger(this.getClass());

    @Override
    public void start(Future<Void> startFuture) {
        vertx.eventBus().consumer("downloadFile", getDownloadHandler());
        startFuture.complete();
    }

    private Handler<Message<String>> getDownloadHandler() {
        return message -> {
            createFile(message);
        };
    }

    private void createFile(Message<String> message) {
        final String filePath = File.separator + "opt/vertx-module" + File.separator + UUID.randomUUID().toString();
        // create the file
        OpenOptions openOptions = new OpenOptions();
        openOptions.setCreateNew(true);
        vertx.fileSystem().open(filePath, openOptions, fileOpenResponse -> {
            if (fileOpenResponse.succeeded()) {
                // open the file
                if (fileOpenResponse.succeeded()) {
                    AsyncFile asyncFile = fileOpenResponse.result();

                    String url = "http://vmpro.mi24.dev/ffh-1-nginx/vertxissue/Testimonial_Thoma.wmv";
                    try {
                        URL resolvedUrl = new URL(url);
                        HttpClientOptions options = new HttpClientOptions()
                                .setDefaultHost(resolvedUrl.getHost());


                        if (resolvedUrl.getPort() != -1) {
                            options.setDefaultPort(resolvedUrl.getPort());
                        }

                        HttpClient client = vertx.createHttpClient(options);
                        HttpClientRequest httpClientRequest = client.get(resolvedUrl.getFile(), getHttpClientResponseHandler(asyncFile, filePath, message));
                        httpClientRequest.exceptionHandler(getExceptionHandler(url, message, asyncFile));
                        httpClientRequest.end();
                    } catch (Exception e) {
                        String errorMessage = String.format("Error while resolving url: %s", url);
                        logger.error(errorMessage, e);
                        message.fail(2, errorMessage);
                    }
                } else {
                    String errorMessage = String.format("Could not open file at : %s", filePath);
                    message.fail(1, errorMessage);
                }
            } else {
                String errorMessage = String.format("Could not create file at : %s", filePath);
                message.fail(1, errorMessage);
            }
        });
    }

    private Handler<Throwable> getExceptionHandler(String url, Message<String> message, AsyncFile asyncFile) {
        return e -> {
            String errorMessage = String.format("Exception during download file: '%s' " + e, url);
            logger.error(errorMessage, e.getCause());
            message.fail(2, errorMessage);
            asyncFile.close();
        };
    }

    private Handler<HttpClientResponse> getHttpClientResponseHandler(final AsyncFile asyncFile, final String filePath, Message<String> message) {
        return httpClientResponse -> {
            int statusCode = httpClientResponse.statusCode();
            if (statusCode != 200) {
                String errorMessage = String.format("Could not download file. Status code was not 200, got: %s", statusCode);
                logger.error(errorMessage);
                asyncFile.close();
                return;
            }

            httpClientResponse.endHandler(httpEndHandler -> {
                asyncFile.close(closeres -> {
                    message.reply(filePath);
                });
            });
            Pump pump = Pump.factory.pump(httpClientResponse, asyncFile);
            pump.start();
        };
    }
}
