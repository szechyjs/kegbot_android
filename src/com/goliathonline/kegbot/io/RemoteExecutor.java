package com.goliathonline.kegbot.io;

import com.goliathonline.kegbot.io.JsonHandler.HandlerException;
import com.goliathonline.kegbot.util.ParserUtils;

import org.apache.http.HttpResponse;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpUriRequest;
import org.json.JSONException;
import org.json.JSONObject;

import android.content.ContentResolver;

import java.io.IOException;
import java.io.InputStream;

/**
 * Executes an {@link HttpUriRequest} and passes the result as an
 * {@link JSONObject} to the given {@link JsonHandler}.
 */
public class RemoteExecutor {
    private final HttpClient mHttpClient;
    private final ContentResolver mResolver;

    public RemoteExecutor(HttpClient httpClient, ContentResolver resolver) {
        mHttpClient = httpClient;
        mResolver = resolver;
    }

    /**
     * Execute a {@link HttpGet} request, passing a valid response through
     * {@link JsonHandler#parseAndApply(JSONObject, ContentResolver)}.
     */
    public void executeGet(String url, JsonHandler handler) throws HandlerException {
        final HttpUriRequest request = new HttpGet(url);
        execute(request, handler);
    }

    /**
     * Execute this {@link HttpUriRequest}, passing a valid response through
     * {@link JsonHandler#parseAndApply(JSONObject, ContentResolver)}.
     */
    public void execute(HttpUriRequest request, JsonHandler handler) throws HandlerException {
        try {
            final HttpResponse resp = mHttpClient.execute(request);
            final int status = resp.getStatusLine().getStatusCode();
            if (status != HttpStatus.SC_OK) {
                throw new HandlerException("Unexpected server response " + resp.getStatusLine()
                        + " for " + request.getRequestLine());
            }

            final InputStream input = resp.getEntity().getContent();
            try {
                final JSONObject parser = ParserUtils.newPullParser(input);
                handler.parseAndApply(parser, mResolver);
            } catch (JSONException e) {
                throw new HandlerException("Malformed response for " + request.getRequestLine(), e);
            } finally {
                if (input != null) input.close();
            }
        } catch (HandlerException e) {
            throw e;
        } catch (IOException e) {
            throw new HandlerException("Problem reading remote response for "
                    + request.getRequestLine(), e);
        }
    }
}

